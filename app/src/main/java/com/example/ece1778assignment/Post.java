package com.example.ece1778assignment;

public class Post implements Comparable<Post>{
    String path;
    String caption;
    String uid;
    String comments;
    String hashtag;
    long timestamp;

    public Post(String uid, String path, String caption, String hashtag, String comments, long timestamp){
        this.uid = uid;
        this.path = path;
        this.caption = caption;
        this.comments = comments;
        this.hashtag = hashtag;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(Post p) {
        return -1*Long.compare(this.timestamp, p.timestamp);
    }
}