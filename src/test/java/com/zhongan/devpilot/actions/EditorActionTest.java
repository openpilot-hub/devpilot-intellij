package com.zhongan.devpilot.actions;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.toolWindow.ToolWindowHeadlessManagerImpl;
import com.zhongan.devpilot.actions.notifications.DevPilotNotification;
import com.zhongan.devpilot.constant.DefaultConst;
import com.zhongan.devpilot.enums.EditorActionEnum;
import com.zhongan.devpilot.enums.SessionTypeEnum;
import com.zhongan.devpilot.gui.toolwindows.DevPilotChatToolWindowFactory;
import com.zhongan.devpilot.gui.toolwindows.chat.DevPilotChatToolWindow;
import com.zhongan.devpilot.gui.toolwindows.components.ContentComponent;
import com.zhongan.devpilot.gui.toolwindows.components.UserChatPanel;
import com.zhongan.devpilot.integrations.llms.aigateway.AIGatewayServiceProvider;
import com.zhongan.devpilot.settings.state.DevPilotLlmSettingsState;
import com.zhongan.devpilot.util.DevPilotMessageBundle;

import javax.swing.JTextPane;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.MockitoAnnotations;

import static com.zhongan.devpilot.util.DevPilotTests.emplacePrompt;
import static com.zhongan.devpilot.util.DevPilotTests.getLast;
import static com.zhongan.devpilot.util.DevPilotTests.getLastContentComponent;
import static com.zhongan.devpilot.util.DevPilotTests.getUserChatPanel;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class EditorActionTest extends BasePlatformTestCase {

    AutoCloseable mockAutoCloseable;

    Project mockedProject;

    AIGatewayServiceProvider mockedLlmProvider;

    ToolWindow mockedPrimaryToolWindow;

    DevPilotChatToolWindow mockedChatToolWindow;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        DevPilotLlmSettingsState.getInstance().setSelectedModel("AIGateway");

        Project project = getProject();
        ToolWindowHeadlessManagerImpl toolWindowManager = new ToolWindowHeadlessManagerImpl(project);
        ServiceContainerUtil.replaceService(project, ToolWindowManager.class, toolWindowManager, getTestRootDisposable());

        mockedPrimaryToolWindow = spy(toolWindowManager.doRegisterToolWindow("DevPilot"));

        mockAutoCloseable = MockitoAnnotations.openMocks(this);
        // mock... :-(
        mockedProject = spy(project);
        mockedLlmProvider = mock(AIGatewayServiceProvider.class);
        when(mockedProject.getService(AIGatewayServiceProvider.class)).thenReturn(mockedLlmProvider);
        mockedChatToolWindow = spy(new DevPilotChatToolWindow(mockedProject, mockedPrimaryToolWindow));
    }

    @Override
    protected void tearDown() throws Exception {
        try {
            mockAutoCloseable.close();
            mockAutoCloseable = null;
            mockedPrimaryToolWindow = null;
            mockedChatToolWindow = null;
            mockedLlmProvider = null;
            mockedProject = null;
        } catch (Throwable e) {
            addSuppressedException(e);
        } finally {
            super.tearDown();
        }
    }

    public void testFix() {
        String code = "try {\n" +
            "    Thread.sleep(1);\n" +
            "} catch (InterruptedException e) { }";
        doTestAction(EditorActionEnum.FIX_THIS, code, "Fix completed!");
    }

    public void testReview() {
        String code = "System.out.println(\"Hello World!\");";
        doTestAction(EditorActionEnum.REVIEW_CODE, code, "Nice code!");
    }

    void doTestAction(EditorActionEnum action, String testCode, String exceptedChatReply) {
        String actionId = DevPilotMessageBundle.get(action.getLabel());

        when(mockedLlmProvider.chatCompletion(any())).thenReturn(exceptedChatReply);

        try (var mockedStaticFactory = mockStatic(DevPilotChatToolWindowFactory.class);
             var mockedStaticNotification = mockStatic(DevPilotNotification.class)) {
            mockedStaticFactory.when(() -> DevPilotChatToolWindowFactory.getDevPilotChatToolWindow(any()))
                .thenReturn(mockedChatToolWindow);

            AnAction reviewAction = ActionManager.getInstance().getAction(actionId);
            assertNotNull(reviewAction);

            // test too long selected
            String hugeCode = RandomStringUtils.randomAlphabetic(DefaultConst.TOKEN_MAX_LENGTH);
            myFixture.configureByText("Test.java", hugeCode);
            myFixture.getEditor().getSelectionModel().setSelection(0, DefaultConst.TOKEN_MAX_LENGTH);
            // fire action event
            myFixture.testAction(reviewAction);
            mockedStaticNotification.verify(() -> DevPilotNotification.info(any()), times(1));
            verify(mockedChatToolWindow, never()).syncSendAndDisplay(any(), any(), any(), any(), any());
            verify(mockedLlmProvider, never()).chatCompletion(any());

            // test normal selected
            myFixture.configureByText("Test.java", testCode);
            myFixture.getEditor().getSelectionModel().setSelection(0, testCode.length());
            // fire action event
            myFixture.testAction(reviewAction);

            String selectedText = myFixture.getEditor().getSelectionModel().getSelectedText();
            assertNotNull(selectedText);
            assertEquals(testCode, selectedText);
            String exceptedPrompt = emplacePrompt(action, selectedText);
            verify(mockedChatToolWindow, times(1)).addClearSessionInfo();
            verify(mockedChatToolWindow, times(1))
                .syncSendAndDisplay(eq(SessionTypeEnum.MULTI_TURN.getCode()), eq(action), eq(exceptedPrompt), any(), any());

            UserChatPanel userChatPanel = getUserChatPanel(mockedChatToolWindow);
            // wait for sending to complete
            PlatformTestUtil.waitWithEventsDispatching("Wait sending timeout",
                () -> !userChatPanel.isSending(), 20);

            System.out.println(userChatPanel.isSending());

            // verify sent message
            verify(mockedLlmProvider, times(1)).chatCompletion(argThat(argument -> {
                assertNotEmpty(argument.getMessages());
                String content = argument.getMessages().get(1).getContent();
                return content.equals(exceptedPrompt);
            }));

            // check reply
            ContentComponent lastContentComponent = getLastContentComponent(mockedChatToolWindow);
            JTextPane displayTextPane = getLast(lastContentComponent);
            assertNotNull(displayTextPane);
            assertTrue(displayTextPane.getText().contains(exceptedChatReply));
        }
    }

}
