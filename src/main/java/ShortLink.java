import java.time.LocalDateTime;
import java.util.UUID;

public class ShortLink {

    private final String originalUrl;
    private final String shortCode;
    private final UUID ownerId;
    private final LocalDateTime createdAt;
    private final LocalDateTime expiresAt;
    private final int maxClicks;
    private int clickCount;
    private LinkStatus status;

    public ShortLink(String originalUrl,
                     String shortCode,
                     UUID ownerId,
                     int maxClicks,
                     int lifetimeHours) {

        this.originalUrl = originalUrl;
        this.shortCode = shortCode;
        this.ownerId = ownerId;
        this.maxClicks = maxClicks;

        this.createdAt = LocalDateTime.now();
        this.expiresAt = createdAt.plusHours(lifetimeHours);

        this.clickCount = 0;
        this.status = LinkStatus.ACTIVE;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public String getShortCode() {
        return shortCode;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public int getMaxClicks() {
        return maxClicks;
    }

    public int getClickCount() {
        return clickCount;
    }

    public LinkStatus getStatus() {
        return status;
    }

    public void setStatus(LinkStatus status) {
        this.status = status;
    }

    public void incrementClicks() {
        clickCount++;

        if (clickCount >= maxClicks) {
            status = LinkStatus.LIMIT_REACHED;
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt)
                || LocalDateTime.now().isEqual(expiresAt);
    }
}
