package com.PrivacyGuard.Plugin;

/**
 * Created by Neil on 2016-12-14.
 */
public class LeakDetectable {
    public String type;
    public String keyword;
    public LeakReport.LeakCategory category;

    public LeakDetectable(String type, String keyword, LeakReport.LeakCategory category) {
        this.type = type;
        this.keyword = keyword;
        this.category = category;
    }
}
