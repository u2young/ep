package xyz.herz.ep.mp.core;

import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.api.impl.WxMpServiceImpl;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.List;

/**
 * 微信公众号 API 门面实现,封装 WxJava SDK ({@code weixin-java-mp})。
 * <p>
 * 使用 {@link WxMpServiceImpl} 单例,生产环境中需通过 {@link WxMpService#setWxMpConfigStorage} 注入真实配置。
 * 测试环境可依赖外部 mock(参见 {@code WeChatApiFacadeSmokeTest})。
 */
@Service
public class WeChatApiFacadeImpl implements WeChatApiFacade {

    private static final Logger log = LoggerFactory.getLogger(WeChatApiFacadeImpl.class);

    private final WxMpService wxMpService = new WxMpServiceImpl();

    @Override
    public String refreshAccessToken(String appId, String secret) {
        try {
            return wxMpService.getAccessToken();
        } catch (WxErrorException e) {
            throw new RuntimeException("刷新 access_token 失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<String> syncFollowers(String accessToken, String nextOpenid) {
        try {
            me.chanjar.weixin.mp.bean.result.WxMpUserList userList =
                    wxMpService.getUserService().userList(nextOpenid);
            return userList.getOpenids();
        } catch (WxErrorException e) {
            log.warn("同步粉丝列表失败,nextOpenid={}", nextOpenid, e);
            return java.util.Collections.emptyList();
        }
    }

    @Override
    public void handleCallback(String appId, String xmlBody, String signature,
                               String timestamp, String nonce,
                               String encryptType, String msgSignature) {
        if (appId == null || appId.isBlank() || xmlBody == null || xmlBody.isBlank()) {
            throw new IllegalArgumentException("appId 和 xmlBody 不能为空");
        }
        try {
            WxMpXmlMessage inMessage = WxMpXmlMessage.fromXml(xmlBody);
            log.info("收到微信回调 type={} fromUser={} msgType={}",
                    inMessage.getMsgType(), inMessage.getFromUser(), inMessage.getMsgType());
        } catch (Exception e) {
            log.error("解析微信回调失败 appId={}", appId, e);
            throw new RuntimeException("解析微信回调失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String uploadTempMedia(String accessToken, String mediaType, byte[] fileBytes, String fileName) {
        try {
            me.chanjar.weixin.common.bean.result.WxMediaUploadResult result =
                    wxMpService.getMaterialService().mediaUpload(mediaType, fileName,
                            new ByteArrayInputStream(fileBytes));
            return result.getMediaId();
        } catch (WxErrorException e) {
            throw new RuntimeException("上传临时素材失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void publishMenu(String accessToken, String menuJson) {
        try {
            wxMpService.getMenuService().menuCreate(menuJson);
        } catch (WxErrorException e) {
            throw new RuntimeException("发布菜单失败: " + e.getMessage(), e);
        }
    }
}
