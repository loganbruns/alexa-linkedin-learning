package linkedinlearning.storage;

public class LearningUserData {

  private String title;

  private String slug;

  private long start;

  private long offset;

  private long totalVideos;

  public LearningUserData() {
    // public no-arg constructor required for DynamoDBMapper marshalling
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public long getStart() {
    return start;
  }

  public void setStart(long start) {
    this.start = start;
  }

  public long getOffset() {
    return offset;
  }

  public void setOffset(Long offset) {
    this.offset = offset;
  }

  public long getTotalVideos() {
    return totalVideos;
  }

  public void setTotalVideos(Long totalVideos) {
    this.totalVideos = totalVideos;
  }
}
