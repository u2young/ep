package xyz.herz.ep.boot.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class SimpleEntityTest {

    private SimpleEntity entity;

    @BeforeEach
    void setUp() {
        entity = new SimpleEntity();
    }

    // ========== 默认值测试 ==========

    @Test
    void testDefaultValues() {
        assertNull(entity.getInput());
        assertNull(entity.getNumber());
        assertNull(entity.getBool());
        assertNull(entity.getDate());
        assertNull(entity.getSlide());
    }

    // ========== Getter / Setter 测试 ==========

    @Test
    void testSetInputAndGetInput() {
        entity.setInput("hello erupt");
        assertEquals("hello erupt", entity.getInput());
    }

    @Test
    void testSetNumberAndGetNumber() {
        entity.setNumber(42.5f);
        assertEquals(42.5f, entity.getNumber(), 0.001f);
    }

    @Test
    void testSetBoolTrueAndGetBool() {
        entity.setBool(true);
        assertTrue(entity.getBool());
    }

    @Test
    void testSetBoolFalseAndGetBool() {
        entity.setBool(false);
        assertFalse(entity.getBool());
    }

    @Test
    void testSetDateAndGetDate() {
        LocalDate today = LocalDate.of(2026, 7, 24);
        entity.setDate(today);
        assertEquals(today, entity.getDate());
    }

    @Test
    void testSetSlideAndGetSlide() {
        entity.setSlide(75);
        assertEquals(75, entity.getSlide());
    }

    // ========== 边界值测试 ==========

    @Test
    void testSetEmptyStringInput() {
        entity.setInput("");
        assertEquals("", entity.getInput());
    }

    @Test
    void testSetMaxSlideValue() {
        entity.setSlide(100);
        assertEquals(100, entity.getSlide());
    }

    @Test
    void testSetMinSlideValue() {
        entity.setSlide(0);
        assertEquals(0, entity.getSlide());
    }

    @Test
    void testSetNegativeNumber() {
        entity.setNumber(-99.9f);
        assertEquals(-99.9f, entity.getNumber(), 0.001f);
    }

    @Test
    void testSetZeroValues() {
        entity.setNumber(0f);
        entity.setSlide(0);
        assertEquals(0f, entity.getNumber(), 0.001f);
        assertEquals(0, entity.getSlide());
    }

    // ========== JPA / Erupt 注解验证测试 ==========

    @Test
    void testEntityHasJpaAnnotations() {
        // 验证类被标记为 JPA Entity
        assertNotNull(entity.getClass().getAnnotation(jakarta.persistence.Entity.class));
    }

    @Test
    void testEntityHasTableAnnotation() {
        // 验证表名映射正确
        jakarta.persistence.Table tableAnno = entity.getClass().getAnnotation(jakarta.persistence.Table.class);
        assertNotNull(tableAnno);
        assertEquals("demo_simple", tableAnno.name());
    }

    @Test
    void testEntityHasEruptAnnotation() {
        // 验证 Erupt 注解存在且名称正确
        xyz.erupt.annotation.Erupt eruptAnno = entity.getClass().getAnnotation(xyz.erupt.annotation.Erupt.class);
        assertNotNull(eruptAnno);
        assertEquals("简单示例", eruptAnno.name());
    }

    @Test
    void testEntityExtendsBaseModel() {
        // 验证继承了 BaseModel
        assertTrue(entity instanceof xyz.erupt.jpa.model.BaseModel);
    }

    // ========== 字段级 Erupt 注解验证 ==========

    @Test
    void testInputFieldHasEruptFieldAnnotation() throws NoSuchFieldException, SecurityException {
        java.lang.reflect.Field field = SimpleEntity.class.getDeclaredField("input");
        var eruptField = field.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(eruptField);
    }

    @Test
    void testNumberFieldHasSortableInView() throws NoSuchFieldException, SecurityException {
        java.lang.reflect.Field field = SimpleEntity.class.getDeclaredField("number");
        var eruptField = field.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(eruptField);
        // views 中 sortable 应该为 true
        assertTrue(eruptField.views()[0].sortable());
    }

    @Test
    void testSlideFieldHasSliderEditType() throws NoSuchFieldException, SecurityException {
        java.lang.reflect.Field field = SimpleEntity.class.getDeclaredField("slide");
        var eruptField = field.getAnnotation(xyz.erupt.annotation.EruptField.class);
        assertNotNull(eruptField);
        xyz.erupt.annotation.sub_field.Edit edit = eruptField.edit();
        assertEquals(xyz.erupt.annotation.sub_field.EditType.SLIDER, edit.type());
    }
}
