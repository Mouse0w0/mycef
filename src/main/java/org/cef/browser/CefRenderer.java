// Copyright (c) 2013 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

// Modified by montoyo for MCEF

package org.cef.browser;

import com.github.mouse0w0.mycef.api.BufferBuilder;
import com.github.mouse0w0.mycef.api.Tessellator;

import java.awt.Rectangle;
import java.nio.ByteBuffer;
import java.util.ArrayList;

//import net.minecraft.client.renderer.BufferBuilder;
//import net.minecraft.client.renderer.GlStateManager;
//import net.minecraft.client.renderer.Tessellator;
//
//import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
//import net.montoyo.mcef.MCEF;
//import net.montoyo.mcef.utilities.Log;
//import org.lwjgl.opengl.EXTBgra;

import static org.lwjgl.opengl.EXTBGRA.GL_BGRA_EXT;
import static org.lwjgl.opengl.GL11.*;

public class CefRenderer {

    //montoyo: debug tool
    private static final ArrayList<Integer> GL_TEXTURES = new ArrayList<>();

    public static void dumpVRAMLeak() {
//        Log.info(">>>>> MCEF: Beginning VRAM leak report");
//        GL_TEXTURES.forEach(tex -> Log.warning(">>>>> MCEF: This texture has not been freed: " + tex));
//        Log.info(">>>>> MCEF: End of VRAM leak report");
    }

    private boolean transparent_;
    public int[] texture_id_ = new int[1];
    private int view_width_ = 0;
    private int view_height_ = 0;
    private Rectangle popup_rect_ = new Rectangle(0, 0, 0, 0);
    private Rectangle original_popup_rect_ = new Rectangle(0, 0, 0, 0);

    protected CefRenderer(boolean transparent) {
        transparent_ = transparent;
        initialize();
    }

    protected boolean isTransparent() {
        return transparent_;
    }

    @SuppressWarnings("static-access")
    protected void initialize() {
        glEnable(GL_TEXTURE_2D);
        texture_id_[0] = glGenTextures();

//        if(MCEF.CHECK_VRAM_LEAK)
//            GL_TEXTURES.add(texture_id_[0]);

        glBindTexture(GL_TEXTURE_2D, texture_id_[0]);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
//        glTexEnvf(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    protected void cleanup() {
        if (texture_id_[0] != 0) {
//            if(MCEF.CHECK_VRAM_LEAK)
//                GL_TEXTURES.remove((Object) texture_id_[0]);

            glDeleteTextures(texture_id_[0]);
        }
    }

    public void render(int x1, int y1, int x2, int y2) {
        if (view_width_ == 0 || view_height_ == 0)
            return;

        Tessellator t = Tessellator.getInstance();
        BufferBuilder builder = t.getBuffer();

        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture_id_[0]);
        builder.begin(GL_QUADS, true, true, true, false);
        builder.pos(x1, y1, 0).tex(0, 0).color(255, 255, 255, 255).endVertex();
        builder.pos(x2, y1, 0).tex(1, 0).color(255, 255, 255, 255).endVertex();
        builder.pos(x2, y2, 0).tex(1, 1).color(255, 255, 255, 255).endVertex();
        builder.pos(x1, y2, 0).tex(0, 1).color(255, 255, 255, 255).endVertex();
        t.draw();
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    protected void onPopupSize(Rectangle rect) {
        if (rect.width <= 0 || rect.height <= 0)
            return;
        original_popup_rect_ = rect;
        popup_rect_ = getPopupRectInWebView(original_popup_rect_);
    }

    protected Rectangle getPopupRectInWebView(Rectangle rc) {
        // if x or y are negative, move them to 0.
        if (rc.x < 0)
            rc.x = 0;
        if (rc.y < 0)
            rc.y = 0;
        // if popup goes outside the view, try to reposition origin
        if (rc.x + rc.width > view_width_)
            rc.x = view_width_ - rc.width;
        if (rc.y + rc.height > view_height_)
            rc.y = view_height_ - rc.height;
        // if x or y became negative, move them to 0 again.
        if (rc.x < 0)
            rc.x = 0;
        if (rc.y < 0)
            rc.y = 0;
        return rc;
    }

    protected void clearPopupRects() {
        popup_rect_.setBounds(0, 0, 0, 0);
        original_popup_rect_.setBounds(0, 0, 0, 0);
    }

    protected void onPaint(boolean popup, Rectangle[] dirtyRects, ByteBuffer buffer, int width, int height, boolean completeReRender) {
        if (transparent_) // Enable alpha blending.
            glEnable(GL_BLEND);

        final int size = (width * height) << 2;
        if (size > buffer.limit()) {
            System.out.println("Bad data passed to CefRenderer.onPaint() triggered safe guards... (1)");
            return;
        }

        // Enable 2D textures.
        glEnable(GL_TEXTURE_2D);
        glBindTexture(GL_TEXTURE_2D, texture_id_[0]);

        int oldAlignement = glGetInteger(GL_UNPACK_ALIGNMENT);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        if (!popup) {
            if (completeReRender || width != view_width_ || height != view_height_) {
                // Update/resize the whole texture.
                view_width_ = width;
                view_height_ = height;
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, view_width_, view_height_, 0, GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
            } else {
                glPixelStorei(GL_UNPACK_ROW_LENGTH, view_width_);

                // Update just the dirty rectangles.
                for (Rectangle rect : dirtyRects) {
                    if (rect.x < 0 || rect.y < 0 || rect.x + rect.width > view_width_ || rect.y + rect.height > view_height_)
                        System.out.println("Bad data passed to CefRenderer.onPaint() triggered safe guards... (2)");
                    else {
                        glPixelStorei(GL_UNPACK_SKIP_PIXELS, rect.x);
                        glPixelStorei(GL_UNPACK_SKIP_ROWS, rect.y);
                        glTexSubImage2D(GL_TEXTURE_2D, 0, rect.x, rect.y, rect.width, rect.height, GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
                    }
                }

                glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
                glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
                glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            }
        } else if (popup_rect_.width > 0 && popup_rect_.height > 0) {
            int skip_pixels = 0, x = popup_rect_.x;
            int skip_rows = 0, y = popup_rect_.y;
            int w = width;
            int h = height;

            // Adjust the popup to fit inside the view.
            if (x < 0) {
                skip_pixels = -x;
                x = 0;
            }
            if (y < 0) {
                skip_rows = -y;
                y = 0;
            }
            if (x + w > view_width_)
                w -= x + w - view_width_;
            if (y + h > view_height_)
                h -= y + h - view_height_;

            // Update the popup rectangle.
            glPixelStorei(GL_UNPACK_ROW_LENGTH, width);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, skip_pixels);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, skip_rows);
            glTexSubImage2D(GL_TEXTURE_2D, 0, x, y, w, h, GL_BGRA_EXT, GL_UNSIGNED_BYTE, buffer);
            glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
            glPixelStorei(GL_UNPACK_SKIP_PIXELS, 0);
            glPixelStorei(GL_UNPACK_SKIP_ROWS, 0);
        }

        glPixelStorei(GL_UNPACK_ALIGNMENT, oldAlignement);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int getViewWidth() {
        return view_width_;
    }

    public int getViewHeight() {
        return view_height_;
    }

}
