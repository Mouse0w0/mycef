// Copyright (c) 2014 The Chromium Embedded Framework Authors. All rights
// reserved. Use of this source code is governed by a BSD-style license that
// can be found in the LICENSE file.

package org.cef.browser;

import com.github.mouse0w0.mycef.api.BrowserRenderer;
import com.github.mouse0w0.mycef.api.MyCefBrowser;
import org.cef.CefClient;

/**
 * Creates a new instance of CefBrowser according the passed values
 */
public class CefBrowserFactory {
    // ===== MyCEF begin =====
    public static MyCefBrowser create(CefClient client, String url, boolean isTransparent, BrowserRenderer renderer, CefRequestContext context) {
        return new CefBrowserOsr(client, url, isTransparent, renderer, context);
    }

//    public static CefBrowser create(CefClient client, String url, boolean isOffscreenRendered,
//            boolean isTransparent, CefRequestContext context) {
//        if (isOffscreenRendered) return new CefBrowserOsr(client, url, isTransparent, context);
//        return new CefBrowserWr(client, url, context);
//    }
    // ===== MyCEF end =====
}
