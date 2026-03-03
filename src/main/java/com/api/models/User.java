package com.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class User {

    @JsonProperty("id")
    private Integer id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("username")
    private String username;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("website")
    private String website;

    // ===================== Constructors =====================

    public User() {}

    public User(String name, String username, String email) {
        this.name     = name;
        this.username = username;
        this.email    = email;
    }

    // ===================== Getters & Setters =====================

    public Integer getId()                      { return id;            }
    public void setId(Integer id)               { this.id = id;         }

    public String getName()                     { return name;          }
    public void setName(String name)            { this.name = name;     }

    public String getUsername()                 { return username;              }
    public void setUsername(String username)    { this.username = username;     }

    public String getEmail()                    { return email;             }
    public void setEmail(String email)          { this.email = email;        }

    public String getPhone()                    { return phone;             }
    public void setPhone(String phone)          { this.phone = phone;        }

    public String getWebsite()                  { return website;               }
    public void setWebsite(String website)      { this.website = website;       }

    @Override
    public String toString() {
        return "User{id=" + id + ", name='" + name + "', username='" +
                username + "', email='" + email + "'}";
    }
}
