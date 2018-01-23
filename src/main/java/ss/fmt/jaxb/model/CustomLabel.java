/*
 * The MIT License
 *
 * Copyright 2018 Pivotal Software, Inc..
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ss.fmt.jaxb.model;

/**
 * Custom label metadata model.
 * @author ss
 */
public class CustomLabel {
    /** Label name. */
    private String fullName;
    /** Description. */
    private String shortDescription;
    /** Value. */
    private String value;
    /** Language. */
    private String language;
    /** Categories. */
    private String categories;
    /**
     * @return the fullName
     */
    public String getFullName() {
        return fullName;
    }
    /**
     * @param fullName the fullName to set
     */
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    /**
     * @return the shortDescription
     */
    public String getShortDescription() {
        return shortDescription;
    }
    /**
     * @param shortDescription the shortDescription to set
     */
    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }
    /**
     * @return the value
     */
    public String getValue() {
        return value;
    }
    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }
    /**
     * @return the language
     */
    public String getLanguage() {
        return language;
    }
    /**
     * @param language the language to set
     */
    public void setLanguage(String language) {
        this.language = language;
    }
    /**
     * @return the categories
     */
    public String getCategories() {
        return categories;
    }
    /**
     * @param categories the categories to set
     */
    public void setCategories(String categories) {
        this.categories = categories;
    }
}
