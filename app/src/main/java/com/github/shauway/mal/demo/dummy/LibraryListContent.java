package com.github.shauway.mal.demo.dummy;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import com.fasterxml.jackson.databind.JavaType;
import com.github.shauway.mal.demo.utils.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Helper class for providing sample title for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class LibraryListContent {
    /**
     * A dummy item representing a piece of title.
     */
    public static class LibraryItem {
        private String title;
        private String cls;

        public LibraryItem() {
        }

        public LibraryItem(String title, String cls) {
            this.title = title;
            this.cls = cls;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getCls() {
            return cls;
        }

        public void setCls(String cls) {
            this.cls = cls;
        }
    }
}
