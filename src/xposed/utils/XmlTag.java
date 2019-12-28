package com.skynet.xposed.utils;

import java.util.ArrayList;
import java.util.HashMap;

public class XmlTag {
  private String mPath;
  private String mName;
  private ArrayList<XmlTag> mChildren = new ArrayList<>();
  private String mContent;

  XmlTag(String path, String name) {
    mPath = path;
    mName = name;
  }

  void addChild(XmlTag tag) {
    mChildren.add(tag);
  }

  String getName() {
    return mName;
  }

  String getContent() {
    return mContent;
  }

  void setContent(String content) {
    boolean hasContent = false;
    if (content != null) {
      for (int i = 0; i < content.length(); ++i) {
        char c = content.charAt(i);
        if ((c != ' ') && (c != '\n')) {
          hasContent = true;
          break;
        }
      }
    }

    if (hasContent) {
      mContent = content;
    }
  }

  ArrayList<XmlTag> getChildren() {
    return mChildren;
  }

  boolean hasChildren() {
    return (mChildren.size() > 0);
  }

  int getChildrenCount() {
    return mChildren.size();
  }

  XmlTag getChild(int index) {
    if ((index >= 0) && (index < mChildren.size())) {
      return mChildren.get(index);
    }
    return null;
  }

  HashMap<String, ArrayList<XmlTag>> getGroupedElements() {
    HashMap<String, ArrayList<XmlTag>> groups = new HashMap<>();
    for (XmlTag child : mChildren) {
      String key = child.getName();
      ArrayList<XmlTag> group = groups.get(key);
      if (group == null) {
        group = new ArrayList<>();
        groups.put(key, group);
      }
      group.add(child);
    }
    return groups;
  }

  String getPath() {
    return mPath;
  }

  @Override
  public String toString() {
    return "XmlTag: " + mName + ", " + mChildren.size() + " children, Content: " + mContent;
  }
}