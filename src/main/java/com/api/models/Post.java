package com.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Post {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("userId")
    private Integer userId;

    @JsonProperty("title")
    private String title;

    @JsonProperty("body")
    private String body;

    // ===================== Constructors =====================

    public Post() {}

    public Post(Integer userId, String title, String body) {
        this.userId = userId;
        this.title  = title;
        this.body   = body;
    }

    public Post(Integer id, Integer userId, String title, String body) {
        this.id     = id;
        this.userId = userId;
        this.title  = title;
        this.body   = body;
    }

    // ===================== Getters & Setters =====================

    public Integer getId()                  { return id;     }
    public void setId(Integer id)           { this.id = id;  }

    public Integer getUserId()              { return userId;       }
    public void setUserId(Integer userId)   { this.userId = userId; }

    public String getTitle()                { return title;         }
    public void setTitle(String title)      { this.title = title;   }

    public String getBody()                 { return body;          }
    public void setBody(String body)        { this.body = body;     }

    @Override
    public String toString() {
        return "Post{id=" + id + ", userId=" + userId +
                ", title='" + title + "', body='" + body + "'}";
    }
}
