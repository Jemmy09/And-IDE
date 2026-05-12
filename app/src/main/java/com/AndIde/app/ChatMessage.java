package com.AndIde.app;

public class ChatMessage {
    private String text;
    private boolean isUser;
    private boolean isImage;
    private String imageUrl;

    public ChatMessage(String text, boolean isUser) {
        this.text = text;
        this.isUser = isUser;
        this.isImage = false;
    }

    public ChatMessage(String imageUrl, boolean isUser, boolean isImage) {
        this.imageUrl = imageUrl;
        this.isUser = isUser;
        this.isImage = isImage;
        this.text = "";
    }

    public String getText() { return text; }
    public boolean isUser() { return isUser; }
    public boolean isImage() { return isImage; }
    public String getImageUrl() { return imageUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessage that = (ChatMessage) o;
        if (isUser != that.isUser) return false;
        if (isImage != that.isImage) return false;
        if (text != null ? !text.equals(that.text) : that.text != null) return false;
        return imageUrl != null ? imageUrl.equals(that.imageUrl) : that.imageUrl == null;
    }

    @Override
    public int hashCode() {
        int result = text != null ? text.hashCode() : 0;
        result = 31 * result + (isUser ? 1 : 0);
        result = 31 * result + (isImage ? 1 : 0);
        result = 31 * result + (imageUrl != null ? imageUrl.hashCode() : 0);
        return result;
    }
}
