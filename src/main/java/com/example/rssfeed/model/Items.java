package com.example.rssfeed.model;

import com.rometools.rome.feed.synd.SyndCategory;
import com.rometools.rome.feed.synd.SyndContent;

import java.util.Date;
import java.util.List;

public class Items {
private String author;
private  List<SyndCategory> categories;
private Date pubDate;
private String comment;
private  SyndContent description;
private String link;

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<SyndCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<SyndCategory> categories) {
        this.categories = categories;
    }

    public Date getPubDate() {
        return pubDate;
    }

    public void setPubDate(Date pubDate) {
        this.pubDate = pubDate;
    }

    public String getComment() {
        return comment;
    }

    public void setComments(String comment) {
        this.comment = comment;
    }

    public SyndContent getDescription() {
        return description;
    }

    public void setDescription(SyndContent description) {
        this.description = description;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Items(String author, List<SyndCategory> catagories, Date date, String comment, SyndContent description, String link){
        setAuthor(author);
        setCategories(catagories);
        setPubDate(date);
        setComments(comment);
        setDescription(description);
        setLink(link);
    }
}
