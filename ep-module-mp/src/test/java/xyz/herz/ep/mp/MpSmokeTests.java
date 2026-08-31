package xyz.herz.ep.mp;

import xyz.herz.ep.mp.entity.MpAccount;
import xyz.herz.ep.mp.entity.MpAutoReply;
import xyz.herz.ep.mp.entity.MpMaterial;
import xyz.herz.ep.mp.entity.MpMenu;
import xyz.herz.ep.mp.entity.MpMessage;
import xyz.herz.ep.mp.entity.MpUser;
import xyz.herz.ep.mp.entity.MpUserTag;
import xyz.herz.ep.mp.enums.MpDictEnums.EnableStatus;
import xyz.herz.ep.mp.enums.MpDictEnums.MaterialType;
import xyz.herz.ep.mp.enums.MpDictEnums.MenuStatus;
import xyz.herz.ep.mp.enums.MpDictEnums.MessageDirection;
import xyz.herz.ep.mp.enums.MpDictEnums.MessageType;
import xyz.herz.ep.mp.enums.MpDictEnums.ReplyContentType;
import xyz.herz.ep.mp.enums.MpDictEnums.ReplyType;
import xyz.herz.ep.mp.enums.MpDictEnums.SubscribeStatus;
import xyz.herz.ep.mp.handler.MpMenuPublishHandler;
import xyz.herz.ep.mp.handler.MpMenuRevokeHandler;
import xyz.herz.ep.mp.handler.MpReplyToggleHandler;
import xyz.herz.ep.mp.jpa.MpAccountRepository;
import xyz.herz.ep.mp.jpa.MpAutoReplyRepository;
import xyz.herz.ep.mp.jpa.MpMaterialRepository;
import xyz.herz.ep.mp.jpa.MpMenuRepository;
import xyz.herz.ep.mp.jpa.MpMessageRepository;
import xyz.herz.ep.mp.jpa.MpUserRepository;
import xyz.herz.ep.mp.jpa.MpUserTagRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公众号(MP)模块冒烟单测:
 * <ol>
 *   <li>账号 CRUD</li>
 *   <li>粉丝 CRUD + 状态切换(关注/取关)</li>
 *   <li>消息记录 CRUD</li>
 *   <li>自动回复 CRUD + 启用/禁用</li>
 *   <li>菜单 CRUD + 发布/撤回状态机</li>
 * </ol>
 */
@SpringBootTest(classes = MpTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@Transactional
@Rollback
class MpSmokeTests {

    @Autowired MpAccountRepository accountRepo;
    @Autowired MpUserRepository userRepo;
    @Autowired MpUserTagRepository tagRepo;
    @Autowired MpMessageRepository messageRepo;
    @Autowired MpAutoReplyRepository replyRepo;
    @Autowired MpMenuRepository menuRepo;
    @Autowired MpMaterialRepository materialRepo;

    @Autowired MpMenuPublishHandler menuPublishHandler;
    @Autowired MpMenuRevokeHandler menuRevokeHandler;
    @Autowired MpReplyToggleHandler replyToggleHandler;

    // =================== 1. 账号 CRUD ===================

    @Test
    void account_crud() {
        MpAccount acc = new MpAccount();
        acc.setName("测试公众号");
        acc.setAppId("wx-test-" + System.nanoTime());
        acc.setAppSecret("secret123");
        acc.setToken("token123");
        acc.setStatus(EnableStatus.ENABLED.code);
        acc.setRemark("冒烟测试账号");
        accountRepo.save(acc);
        assertNotNull(acc.getId());

        // read
        MpAccount loaded = accountRepo.findById(acc.getId()).orElseThrow();
        assertEquals("测试公众号", loaded.getName());
        assertEquals(EnableStatus.ENABLED.code, loaded.getStatus());

        // update
        loaded.setName("测试公众号-改");
        loaded.setStatus(EnableStatus.DISABLED.code);
        accountRepo.save(loaded);
        MpAccount updated = accountRepo.findById(acc.getId()).orElseThrow();
        assertEquals("测试公众号-改", updated.getName());
        assertEquals(EnableStatus.DISABLED.code, updated.getStatus());

        // delete
        accountRepo.delete(updated);
        assertTrue(accountRepo.findById(acc.getId()).isEmpty());

        // findByAppId
        MpAccount a2 = new MpAccount();
        a2.setName("二号公众号");
        a2.setAppId("wx-findbyid");
        a2.setAppSecret("s2");
        accountRepo.save(a2);
        assertEquals("二号公众号", accountRepo.findByAppId("wx-findbyid").orElseThrow().getName());
    }

    // =================== 2. 粉丝 CRUD + 状态切换(关注/取关) ===================

    @Test
    void user_crud_and_subscribe_toggle() {
        MpAccount acc = basicAccount();

        // create (未关注)
        MpUser user = new MpUser();
        user.setAccount(acc);
        user.setOpenId("oTest_" + System.nanoTime());
        user.setNickname("张三");
        user.setSex(1);
        user.setCity("北京");
        user.setSubscribeStatus(SubscribeStatus.UNSUBSCRIBED.code);
        userRepo.save(user);
        assertNotNull(user.getId());
        assertEquals(SubscribeStatus.UNSUBSCRIBED.code, user.getSubscribeStatus());

        // 关注:0 → 1
        MpUser loaded = userRepo.findById(user.getId()).orElseThrow();
        loaded.setSubscribeStatus(SubscribeStatus.SUBSCRIBED.code);
        loaded.setSubscribeTime(LocalDateTime.now());
        userRepo.save(loaded);
        MpUser subscribed = userRepo.findById(user.getId()).orElseThrow();
        assertEquals(SubscribeStatus.SUBSCRIBED.code, subscribed.getSubscribeStatus());
        assertNotNull(subscribed.getSubscribeTime());

        // 取关:1 → 0
        subscribed.setSubscribeStatus(SubscribeStatus.UNSUBSCRIBED.code);
        subscribed.setUnsubscribeTime(LocalDateTime.now());
        userRepo.save(subscribed);
        MpUser unsubscribed = userRepo.findById(user.getId()).orElseThrow();
        assertEquals(SubscribeStatus.UNSUBSCRIBED.code, unsubscribed.getSubscribeStatus());
        assertNotNull(unsubscribed.getUnsubscribeTime());

        // 重新关注:覆盖 subscribeTime,清空 unsubscribeTime
        unsubscribed.setSubscribeStatus(SubscribeStatus.SUBSCRIBED.code);
        unsubscribed.setSubscribeTime(LocalDateTime.now());
        unsubscribed.setUnsubscribeTime(null);
        userRepo.save(unsubscribed);
        MpUser reSub = userRepo.findById(user.getId()).orElseThrow();
        assertEquals(SubscribeStatus.SUBSCRIBED.code, reSub.getSubscribeStatus());
        assertNull(reSub.getUnsubscribeTime());

        // findByOpenId
        assertEquals(user.getId(), userRepo.findByOpenId(user.getOpenId()).orElseThrow().getId());

        // delete
        userRepo.delete(reSub);
        assertTrue(userRepo.findById(user.getId()).isEmpty());
    }

    // =================== 3. 消息记录 CRUD ===================

    @Test
    void message_crud() {
        MpAccount acc = basicAccount();

        MpMessage msg = new MpMessage();
        msg.setAccount(acc);
        msg.setFromUser("oFromUser");
        msg.setToUser("gh_toUser");
        msg.setMsgType(MessageType.TEXT.code);
        msg.setContent("你好,公众号");
        msg.setDirection(MessageDirection.RECEIVE.code);
        msg.setCreateTime(LocalDateTime.now());
        messageRepo.save(msg);
        assertNotNull(msg.getId());

        MpMessage loaded = messageRepo.findById(msg.getId()).orElseThrow();
        assertEquals(MessageType.TEXT.code, loaded.getMsgType());
        assertEquals(MessageDirection.RECEIVE.code, loaded.getDirection());
        assertEquals("你好,公众号", loaded.getContent());

        // update direction (公众号回复粉丝)
        loaded.setDirection(MessageDirection.SEND.code);
        loaded.setContent("你好,欢迎使用");
        messageRepo.save(loaded);
        MpMessage updated = messageRepo.findById(msg.getId()).orElseThrow();
        assertEquals(MessageDirection.SEND.code, updated.getDirection());

        messageRepo.delete(updated);
        assertTrue(messageRepo.findById(msg.getId()).isEmpty());
    }

    // =================== 4. 自动回复 CRUD + 启用/禁用 ===================

    @Test
    void auto_reply_crud_and_toggle() {
        MpAccount acc = basicAccount();

        // create (默认禁用)
        MpAutoReply reply = new MpAutoReply();
        reply.setAccount(acc);
        reply.setType(ReplyType.KEYWORD.code);
        reply.setMatchKeyword("你好");
        reply.setReplyContentType(ReplyContentType.TEXT.code);
        reply.setReplyContent("你好,我是小助手");
        reply.setStatus(EnableStatus.DISABLED.code);
        reply.setSort(10);
        replyRepo.save(reply);
        assertNotNull(reply.getId());
        assertEquals(EnableStatus.DISABLED.code, reply.getStatus());

        // 启用:operationParam = ENABLE
        String r1 = replyToggleHandler.exec(List.of(reply), null, new String[]{ MpReplyToggleHandler.ENABLE });
        assertTrue(r1.contains("成功 1"), () -> "启用失败:" + r1);
        assertEquals(EnableStatus.ENABLED.code,
            replyRepo.findById(reply.getId()).orElseThrow().getStatus());

        // 再次启用应跳过
        String r1b = replyToggleHandler.exec(List.of(reply), null, new String[]{ MpReplyToggleHandler.ENABLE });
        assertTrue(r1b.contains("跳过 1"), () -> "重复启用应跳过:" + r1b);

        // 禁用:operationParam = DISABLE
        String r2 = replyToggleHandler.exec(List.of(reply), null, new String[]{ MpReplyToggleHandler.DISABLE });
        assertTrue(r2.contains("成功 1"), () -> "禁用失败:" + r2);
        assertEquals(EnableStatus.DISABLED.code,
            replyRepo.findById(reply.getId()).orElseThrow().getStatus());

        // delete
        replyRepo.delete(reply);
        assertTrue(replyRepo.findById(reply.getId()).isEmpty());

        // 顺便验证标签 CRUD(预留字段)
        MpUserTag tag = new MpUserTag();
        tag.setAccount(acc);
        tag.setName("VIP粉丝");
        tag.setUserIds("1,2,3");
        tagRepo.save(tag);
        assertNotNull(tag.getId());
        assertEquals("VIP粉丝", tagRepo.findById(tag.getId()).orElseThrow().getName());

        // 素材 CRUD
        MpMaterial mat = new MpMaterial();
        mat.setAccount(acc);
        mat.setType(MaterialType.IMAGE.code);
        mat.setMediaId("media_001");
        mat.setUrl("http://example.com/1.png");
        mat.setName("二维码.png");
        materialRepo.save(mat);
        assertNotNull(mat.getId());
        assertEquals(MaterialType.IMAGE.code, materialRepo.findById(mat.getId()).orElseThrow().getType());
    }

    // =================== 5. 菜单 CRUD + 发布/撤回状态机 ===================

    @Test
    void menu_crud_and_publish_revoke_state_machine() {
        MpAccount acc = basicAccount();

        // create (草稿)
        MpMenu menu = new MpMenu();
        menu.setAccount(acc);
        menu.setParentId(0L);
        menu.setName("主菜单-关于我们");
        menu.setType("view");
        menu.setUrl("http://example.com/about");
        menu.setSort(1);
        menu.setStatus(MenuStatus.DRAFT.code);
        menuRepo.save(menu);
        assertNotNull(menu.getId());
        assertEquals(MenuStatus.DRAFT.code, menu.getStatus());

        // 发布:0 → 1
        String r1 = menuPublishHandler.exec(List.of(menu), null, null);
        assertTrue(r1.contains("成功 1"), () -> "发布失败:" + r1);
        assertEquals(MenuStatus.PUBLISHED.code,
            menuRepo.findById(menu.getId()).orElseThrow().getStatus());

        // 重复发布应跳过
        String r1b = menuPublishHandler.exec(List.of(menu), null, null);
        assertTrue(r1b.contains("跳过 1"), () -> "重复发布应跳过:" + r1b);

        // 撤回:1 → 0
        String r2 = menuRevokeHandler.exec(List.of(menu), null, null);
        assertTrue(r2.contains("成功 1"), () -> "撤回失败:" + r2);
        assertEquals(MenuStatus.DRAFT.code,
            menuRepo.findById(menu.getId()).orElseThrow().getStatus());

        // 重复撤回应跳过
        String r2b = menuRevokeHandler.exec(List.of(menu), null, null);
        assertTrue(r2b.contains("跳过 1"), () -> "重复撤回应跳过:" + r2b);

        // 二级菜单(click 类型)
        MpMenu sub = new MpMenu();
        sub.setAccount(acc);
        sub.setParentId(menu.getId());
        sub.setName("子菜单-联系我们");
        sub.setType("click");
        sub.setMenuKey("V1001_CONTACT");
        sub.setSort(1);
        sub.setStatus(MenuStatus.DRAFT.code);
        menuRepo.save(sub);
        assertEquals(menu.getId(), sub.getParentId());

        // 批量发布(主+子)
        String r3 = menuPublishHandler.exec(List.of(menu, sub), null, null);
        assertTrue(r3.contains("成功 2"), () -> "批量发布失败:" + r3);
        assertEquals(MenuStatus.PUBLISHED.code,
            menuRepo.findById(menu.getId()).orElseThrow().getStatus());
        assertEquals(MenuStatus.PUBLISHED.code,
            menuRepo.findById(sub.getId()).orElseThrow().getStatus());

        // delete
        menuRepo.deleteAll(List.of(menu, sub));
        assertTrue(menuRepo.findById(menu.getId()).isEmpty());
    }

    // =================== TR-2.1 PASSWORD 掩码注解(RED→GREEN) ===================

    @Test
    void password_masked_for_appsecret_and_token() throws NoSuchFieldException {
        // RED→GREEN: MpAccount.appSecret / token 字段视图+编辑均必须为 PASSWORD 掩码
        java.lang.reflect.Field appSecretField = MpAccount.class.getDeclaredField("appSecret");
        xyz.erupt.annotation.EruptField appSecretAnn =
            appSecretField.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(appSecretAnn, "appSecret 应有 @EruptField");
        assertEquals(xyz.erupt.annotation.sub_field.ViewType.PASSWORD,
            appSecretAnn.views()[0].type(),
            "appSecret 视图类型应为 PASSWORD 掩码,防止敏感信息明文展示");
        assertEquals(xyz.erupt.annotation.sub_field.EditType.PASSWORD,
            appSecretAnn.edit().type(),
            "appSecret 编辑类型应为 PASSWORD 掩码,表单显示占位符保留原值");

        java.lang.reflect.Field tokenField = MpAccount.class.getDeclaredField("token");
        xyz.erupt.annotation.EruptField tokenAnn =
            tokenField.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(tokenAnn, "token 应有 @EruptField");
        assertEquals(xyz.erupt.annotation.sub_field.ViewType.PASSWORD,
            tokenAnn.views()[0].type(),
            "token 视图类型应为 PASSWORD 掩码");
        assertEquals(xyz.erupt.annotation.sub_field.EditType.PASSWORD,
            tokenAnn.edit().type(),
            "token 编辑类型应为 PASSWORD 掩码");
    }

    // =================== TR-3.1/3.2 @DragSort 拖拽排序(RED→GREEN) ===================

    @Test
    void mp_menu_drag_sort_annotation_and_order() throws Exception {
        // TR-3.2 注解断言: @Erupt.dragSort.field 必须 = "sort"
        xyz.erupt.annotation.Erupt eruptAnn =
            MpMenu.class.getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertNotNull(eruptAnn, "MpMenu 应有 @Erupt");
        assertEquals("sort", eruptAnn.dragSort().field(),
            "MpMenu 必须配置 @DragSort(field=\"sort\") 支持列表拖拽排序");

        // TR-3.1 字段完整性: sort 必须是 Integer,默认值 0
        MpMenu empty = new MpMenu();
        java.lang.reflect.Field sortF = MpMenu.class.getDeclaredField("sort");
        sortF.setAccessible(true);
        assertEquals(Integer.class, sortF.getType(), "sort 字段类型必须是 Integer");
        assertEquals(0, sortF.get(empty), "sort 默认值应为 0");

        // TR-3.1 排序查询: save 3 条(sort=30/10/20),按 sort ASC 查询顺序应为 10,20,30
        MpAccount acc = basicAccount();
        MpMenu m30 = new MpMenu();
        m30.setAccount(acc); m30.setName("sort-30"); m30.setSort(30);
        m30.setStatus(MenuStatus.DRAFT.code);
        MpMenu m10 = new MpMenu();
        m10.setAccount(acc); m10.setName("sort-10"); m10.setSort(10);
        m10.setStatus(MenuStatus.DRAFT.code);
        MpMenu m20 = new MpMenu();
        m20.setAccount(acc); m20.setName("sort-20"); m20.setSort(20);
        m20.setStatus(MenuStatus.DRAFT.code);
        menuRepo.saveAll(List.of(m30, m10, m20));
        // 按 sort ASC 查(通过 JpaRepository 默认 findAll + 排序器或手工 stream 排)
        List<MpMenu> ordered = menuRepo.findAll().stream()
            .filter(m -> acc.equals(m.getAccount()))
            .sorted((a, b) -> Integer.compare(a.getSort(), b.getSort()))
            .toList();
        assertEquals(3, ordered.size(), "该账号下应有 3 个菜单");
        assertEquals("sort-10", ordered.get(0).getName(), "sort ASC 第一个应为 10");
        assertEquals("sort-20", ordered.get(1).getName(), "sort ASC 第二个应为 20");
        assertEquals("sort-30", ordered.get(2).getName(), "sort ASC 第三个应为 30");
    }

    // =================== helpers ===================

    private MpAccount basicAccount() {
        MpAccount acc = new MpAccount();
        acc.setName("冒烟公众号-" + System.nanoTime());
        acc.setAppId("wx-smoke-" + System.nanoTime());
        acc.setAppSecret("smoke-secret");
        acc.setToken("smoke-token");
        acc.setStatus(EnableStatus.ENABLED.code);
        accountRepo.save(acc);
        return acc;
    }
}
