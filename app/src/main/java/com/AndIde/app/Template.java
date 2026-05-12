package com.AndIde.app;

public class Template {
    private String name;
    private String description;
    private String code;
    private String fileName;

    public Template(String name, String description, String code, String fileName) {
        this.name = name;
        this.description = description;
        this.code = code;
        this.fileName = fileName;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCode() { return code; }
    public String getFileName() { return fileName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Template template = (Template) o;
        if (name != null ? !name.equals(template.name) : template.name != null) return false;
        if (description != null ? !description.equals(template.description) : template.description != null) return false;
        if (code != null ? !code.equals(template.code) : template.code != null) return false;
        return fileName != null ? fileName.equals(template.fileName) : template.fileName == null;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (code != null ? code.hashCode() : 0);
        result = 31 * result + (fileName != null ? fileName.hashCode() : 0);
        return result;
    }
}