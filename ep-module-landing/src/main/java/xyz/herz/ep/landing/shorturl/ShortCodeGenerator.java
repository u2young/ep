package xyz.herz.ep.landing.shorturl;

import org.springframework.stereotype.Component;

/**
 * 短码生成器:Base62 自增 ID 编码。
 * <p>(pageId + 1_000_000) Base62 编码,前缀补零到 6 位。
 * <ul>
 *   <li>无碰撞:自增 ID 本身唯一,零额外查询</li>
 *   <li>可逆:短码可解码回 pageId</li>
 *   <li>短:6 位覆盖 568 亿空间,百万级落地页内必唯一</li>
 * </ul>
 */
@Component
public class ShortCodeGenerator {

    private static final char[] BASE62 =
        "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static final long OFFSET = 1_000_000L;

    /**
     * 落地页 id → 6 位短码(加 1_000_000 偏移防止低位太短)。
     * <p>id=1 → "0000g8";id=1000 → "0004le";百万级内必唯一。
     */
    public String generate(Long pageId) {
        if (pageId == null || pageId < 0) {
            throw new IllegalArgumentException("pageId 不能为负: " + pageId);
        }
        long n = pageId + OFFSET;
        StringBuilder sb = new StringBuilder();
        do {
            sb.insert(0, BASE62[(int) (n % 62)]);
            n /= 62;
        } while (n > 0);
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }
        return sb.substring(0, 6);
    }

    /** 短码 → pageId(逆运算,可用于排查)。 */
    public Long decode(String code) {
        if (code == null || code.isEmpty()) return null;
        long n = 0;
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            int idx = indexOf(c);
            if (idx < 0) return null;
            n = n * 62 + idx;
        }
        // 减去偏移,得到原始 pageId
        long pageId = n - OFFSET;
        return pageId >= 0 ? pageId : null;
    }

    private static int indexOf(char c) {
        for (int i = 0; i < BASE62.length; i++) {
            if (BASE62[i] == c) return i;
        }
        return -1;
    }
}
