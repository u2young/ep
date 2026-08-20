package xyz.herz.ep.landing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import xyz.herz.ep.landing.entity.LandingTemplate;
import xyz.herz.ep.landing.enums.LandingDictEnums.EnableStatus;
import xyz.herz.ep.landing.enums.LandingDictEnums.TemplateCategory;
import xyz.herz.ep.landing.jpa.LandingTemplateRepository;

import java.util.List;

/**
 * 落地页模板初始化器。
 * <p>启动时若模板表为空,插入 3 个预设 amis schema 模板(空白/留资表单/产品介绍),
 * 让系统开箱即用。已存在数据则跳过(幂等)。
 */
@Component
public class LandingTemplateInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LandingTemplateInitializer.class);

    @Autowired private LandingTemplateRepository tplRepo;

    @Override
    public void run(String... args) {
        if (tplRepo.count() > 0) {
            log.debug("落地页模板已存在({}),跳过初始化", tplRepo.count());
            return;
        }
        log.info("初始化落地页预设模板...");
        initBlank();
        initFormLead();
        initProduct();
        log.info("落地页预设模板初始化完成,共 {} 个", tplRepo.count());
    }

    private void save(String name, int category, String schema, String thumb) {
        LandingTemplate t = new LandingTemplate();
        t.setName(name);
        t.setCategory(category);
        t.setSchema(schema);
        t.setThumbUrl(thumb);
        t.setEnabled(EnableStatus.ENABLED.code);
        tplRepo.save(t);
    }

    /** 空白页模板。 */
    private void initBlank() {
        String schema = "{\"type\":\"page\",\"title\":\"空白页\",\"body\":[{\"type\":\"tpl\","
            + "\"tpl\":\"<h1 style=\\\"text-align:center\\\">在此编辑您的落地页</h1>\"}]}";
        save("空白页", TemplateCategory.BLANK.code, schema, null);
    }

    /** 留资表单模板。 */
    private void initFormLead() {
        String schema = "{\"type\":\"page\",\"title\":\"留资表单\",\"body\":[{\"type\":\"form\","
            + "\"title\":\"预约咨询\",\"api\":{\"method\":\"post\",\"url\":\"/saveLead\"},"
            + "\"body\":[{\"type\":\"input-text\",\"name\":\"name\",\"label\":\"姓名\",\"required\":true},"
            + "{\"type\":\"input-mobile\",\"name\":\"phone\",\"label\":\"手机号\",\"required\":true},"
            + "{\"type\":\"textarea\",\"name\":\"remark\",\"label\":\"备注\"}]}]}";
        save("留资表单", TemplateCategory.FORM_LEAD.code, schema, null);
    }

    /** 产品介绍模板。 */
    private void initProduct() {
        String schema = "{\"type\":\"page\",\"title\":\"产品介绍\",\"body\":["
            + "{\"type\":\"tpl\",\"tpl\":\"<div style='text-align:center'>\"},"
            + "{\"type\":\"image\",\"src\":\"https://via.placeholder.com/600x300\","
            + "\"imageMode\":\"original\"},"
            + "{\"type\":\"tpl\",\"tpl\":\"<h2>产品名称</h2><p>产品介绍文字</p>\"},"
            + "{\"type\":\"button\",\"label\":\"立即咨询\",\"actionType\":\"ajax\","
            + "\"api\":{\"method\":\"post\",\"url\":\"/saveLead\",\"data\":{\"pageId\":1}}"
            + "}]}";
        save("产品介绍", TemplateCategory.PRODUCT.code, schema, null);
    }
}
