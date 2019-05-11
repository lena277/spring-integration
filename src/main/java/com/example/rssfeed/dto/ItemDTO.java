package com.example.rssfeed.dto;

import javax.xml.bind.annotation.*;
import java.util.Date;

@XmlRootElement(name = "items")
public class ItemDTO {

    private String uri;
    private String categories;
    private Date pubDate;
    private String comment;
    private String description;
    private String link;

    @XmlElement(name = "uri")
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    @XmlElement(name = "categories")
    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    @XmlElement(name = "punDate")
    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    @XmlElement(name = "comment")
    public String getComment() {
        return comment;
    }

    public void setComments(String comment) {
        this.comment = comment;
    }

    @XmlElement(name = "desceiption")
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @XmlElement(name = "link")
    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public ItemDTO(String uri, String catagories, Date date, String comment, String description, String link){
        setUri(uri);
        setCategories(catagories);
        setPubDate(date);
        setComments(comment);
        setDescription(description);
        setLink(link);
    }
    public ItemDTO(){

    }
}
