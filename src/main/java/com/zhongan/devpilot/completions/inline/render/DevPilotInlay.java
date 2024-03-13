//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.zhongan.devpilot.completions.inline.render;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Editor;
import com.zhongan.devpilot.completions.prediction.DevPilotCompletion;
import java.awt.Rectangle;

public interface DevPilotInlay extends Disposable {
    static DevPilotInlay create(Disposable parent) {
        return new DefaultDevPilotInlay(parent);
    }

    Integer getOffset();

    Boolean isEmpty();

    Rectangle getBounds();

    void render(Editor var1, DevPilotCompletion var2, int var3);
}
