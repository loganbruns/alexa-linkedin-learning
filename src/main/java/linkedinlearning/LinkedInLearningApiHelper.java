package linkedinlearning;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import javax.net.ssl.HttpsURLConnection;

public class LinkedInLearningApiHelper {

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
  static class Course {
    @JsonProperty("title")
    String title;

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
  static class LearningPath {
    @JsonProperty("title")
    String title;

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
  static class Element {
    @JsonProperty("trackingId")
    String trackingId;

    @JsonProperty("hitInfo")
    HitInfo hitInfo;

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
    URL url = new URL("https://www.linkedin.com/learning-api/search?q=search&entityType=" + category + "&keywords=" + keywords);

    HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();

    conn.setRequestMethod("GET");
    conn.setRequestProperty("Cookie", "JSESSIONID=csrf");
    conn.setRequestProperty("Csrf-Token", "csrf");

    return new ObjectMapper().readValue(conn.getInputStream(), SearchResults.class);
  }

  public static List<String> summarize(SearchResults results, String category) {
    if ("VIDEO".equals(category)) {
      return results.elements.stream().map(el -> el.hitInfo.searchVideo.video.course.title).collect(Collectors.toList());
    } else if ("LEARNING_PATH".equals(category)) {
      return results.elements.stream().map(el -> el.hitInfo.searchLearningPath.learningPath.title).collect(Collectors.toList());
    } else {
      return results.elements.stream().map(el -> el.hitInfo.searchCourse.course.title).collect(Collectors.toList());
    }
  }

  public static void main(String[] args) throws Exception {
    SearchResults results = search(args[0], "");
    System.out.println("Results:\n" + results);
    System.out.println("Summary:\n" + summarize(results, args[0]));
  }
}
