package linkedinlearning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;

public class LinkedInLearningApiHelper {

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Content {
    
    @JsonProperty("title")
    public String title;
    
    @JsonProperty("slug")
    public String slug;

    @Override
    public String toString() {
      return ToStringBuilder.reflectionToString(this);
    }
  }    

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Paging {
    
    @JsonProperty("start")
    private Long start;

    @JsonProperty("count")
    private Long count;

    @JsonProperty("total")
    private Long total;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Course extends Content {
    @JsonProperty("description")
    String description;

    @JsonProperty("shortDescription")
    String shortDescription;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SearchCourse {
    @JsonProperty("course")
    Course course;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Video {
    @JsonProperty("course")
    Course course;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SearchVideo {
    @JsonProperty("video")
    Video video;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class LearningPath extends Content {
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SearchLearningPath {
    @JsonProperty("learningPath")
    LearningPath learningPath;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class HitInfo {
    @JsonProperty("com.linkedin.learning.api.search.SearchCourse")
    SearchCourse searchCourse;

    @JsonProperty("com.linkedin.learning.api.search.SearchVideo")
    SearchVideo searchVideo;

    @JsonProperty("com.linkedin.learning.api.search.SearchLearningPath")
    SearchLearningPath searchLearningPath;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }
  
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SelectedVideo {
    @JsonProperty("url")
    Url url;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }
  
  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Url {
    @JsonProperty("progressiveUrl")
    String progressiveUrl;

    @JsonProperty("streamingUrl")
    String streamingUrl;

    @JsonProperty("expiresAt")
    Long expiresAt;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class Element {
    @JsonProperty("trackingId")
    String trackingId;

    @JsonProperty("hitInfo")
    HitInfo hitInfo;

    @JsonProperty("selectedVideo")
    SelectedVideo selectedVideo;

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  @JsonInclude(Include.NON_NULL)
  @JsonIgnoreProperties(ignoreUnknown = true)
  static class SearchResults {

    @JsonProperty("paging")
    Paging paging;

    @JsonProperty("elements")
    List<Element> elements;
    
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
  }

  public static SearchResults search(String category, String keywords) throws IOException {
    URL url = new URL("https://www.linkedin.com/learning-api/search?q=search&entityType=" +
		      URLEncoder.encode(category, "UTF-8") +
		      "&keywords=" + URLEncoder.encode(keywords, "UTF-8"));

    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

    conn.setRequestMethod("GET");
    conn.setRequestProperty("Cookie", "JSESSIONID=csrf");
    conn.setRequestProperty("Csrf-Token", "csrf");

    return new ObjectMapper().readValue(conn.getInputStream(), SearchResults.class);
  }

  public static List<Content> summarize(SearchResults results, String category) {
    if ("VIDEO".equals(category)) {
      return results.elements.stream().map(el -> el.hitInfo.searchVideo.video.course).collect(Collectors.toList());
    } else if ("LEARNING_PATH".equals(category)) {
      return results.elements.stream().map(el -> el.hitInfo.searchLearningPath.learningPath).collect(Collectors.toList());
    } else {
      return results.elements.stream().map(el -> el.hitInfo.searchCourse.course).collect(Collectors.toList());
    }
  }

  public static SearchResults searchCourses(String slug) throws IOException {
    URL url = new URL("https://www.linkedin.com/learning-api/detailedCourses?courseSlug=" +
		      URLEncoder.encode(slug, "UTF-8") + "&q=slugs");

    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

    conn.setRequestMethod("GET");
    conn.setRequestProperty("Cookie", "JSESSIONID=csrf");
    conn.setRequestProperty("Csrf-Token", "csrf");

    return new ObjectMapper().readValue(conn.getInputStream(), SearchResults.class);
  }

  public static String getPlaybackUrl(String slug) throws IOException {
    return searchCourses(slug).elements.get(0).selectedVideo.url.progressiveUrl;
  }

  public static void main(String[] args) throws Exception {
    SearchResults results = search(args[0], args[1]);
    System.out.println("Results:\n" + results);
    List<Content> contents = summarize(results, args[0]);
    System.out.println("Summary:\n" + contents);
    System.out.println("Url: " + getPlaybackUrl(contents.get(0).slug));
  }
}
