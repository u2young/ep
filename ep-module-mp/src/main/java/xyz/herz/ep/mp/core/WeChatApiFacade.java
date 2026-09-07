package xyz.herz.ep.mp.core;

/**
 * 微信公众号 API 门面。
 * <p>
 * 封装 WxJava SDK ({@code com.github.binarywang:weixin-java-mp}) 的核心能力,
 * 对外隐藏第三方依赖,便于测试替换和后期扩展。
 * <p>
 * 核心功能:
 * <ul>
 *   <li>access_token 自动刷新与缓存</li>
 *   <li>粉丝列表同步(支持分页)</li>
 *   <li>消息事件回调解析(关注/取消关注/扫码/菜单点击等)</li>
 *   <li>素材管理(临时/永久)</li>
 *   <li>自定义菜单发布/撤销</li>
 * </ul>
 *
 * @see <a href="https://github.com/wechat-miniprogram/weixin-java-tools">WxJava 官方文档</a>
 */
public interface WeChatApiFacade {

    /**
     * 刷新指定公众号的 access_token。
     *
     * @param appId  公众号 AppID
     * @param secret 公众号 AppSecret
     * @return 新的 access_token,失败抛 {@link RuntimeException}
     */
    String refreshAccessToken(String appId, String secret);

    /**
     * 获取公众号粉丝列表(分页)。
     *
     * @param accessToken access_token
     * @param nextOpenid  下一页游标,首次传 null
     * @return 粉丝 openId 列表,返回为空说明已读完
     */
    java.util.List<String> syncFollowers(String accessToken, String nextOpenid);

    /**
     * 解析微信消息回调 XML 并写入业务模型。
     * <p>
     * 支持事件类消息(关注/取消关注/扫码)和文本/图片消息。
     *
     * @param appId        公众号 AppID
     * @param xmlBody      微信推送的 XML 原文
     * @param signature    微信签名(外部需校验)
     * @param timestamp    微信时间戳
     * @param nonce        微信随机数
     * @param encryptType  加密方式(明文/混合/AES),当前仅支持 "plaintext"
     * @param msgSignature 消息签名
     * @throws IllegalArgumentException 当 appId 或 xmlBody 为空时抛出
     */
    void handleCallback(String appId, String xmlBody, String signature,
                        String timestamp, String nonce,
                        String encryptType, String msgSignature);

    /**
     * 上传临时素材(有效期 3 天)。
     *
     * @param accessToken access_token
     * @param mediaType   素材类型: photo / voice / video / thumb
     * @param fileBytes   文件二进制
     * @param fileName    文件名(用于扩展名识别)
     * @return 媒体 ID
     */
    String uploadTempMedia(String accessToken, String mediaType, byte[] fileBytes, String fileName);

    /**
     * 发布自定义菜单。
     *
     * @param accessToken access_token
     * @param menuJson    菜单 JSON(符合微信菜单规范)
     */
    void publishMenu(String accessToken, String menuJson);
}
