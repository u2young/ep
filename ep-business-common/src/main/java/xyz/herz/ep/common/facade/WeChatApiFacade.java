package xyz.herz.ep.common.facade;

import java.util.List;

/**
 * 微信公众号 API Facade。
 * MP 模块通过此接口调用微信能力，
 * 测试环境使用 Stub 实现，生产环境替换为 WxJava 实现。
 */
public interface WeChatApiFacade {

    /** 获取 Access Token */
    String getAccessToken(Long accountId);

    /** 同步粉丝列表 */
    List<String> syncUserList(Long accountId);

    /** 发送客服消息 */
    boolean sendCustomMessage(Long accountId, String openId, String msgType, String content);

    /** 发布自定义菜单 */
    boolean publishMenu(Long accountId, String menuJson);

    /** 上传临时素材 */
    String uploadMaterial(Long accountId, String type, byte[] data);
}
