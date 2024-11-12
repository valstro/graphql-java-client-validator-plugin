package com.valstro.plugin.generated;

public class Tag {

    private String id;
    private TagCode tagCode;

    public Tag() {}

    public Tag(String id, TagCode tagCode) {
        this.id = id;
        this.tagCode = tagCode;
    }

    public Tag(String id, String tagCode) {
        this.id = id;
        this.tagCode = TagCode.valueOf(tagCode);
    }

    public String getId() {
        return id;
    }

    public TagCode getTagCode() {
        return tagCode;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTagCode(TagCode tagCode) {
        this.tagCode = tagCode;
    }
}
