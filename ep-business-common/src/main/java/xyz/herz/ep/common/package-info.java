/**
 * 跨业务模块的通用抽象。
 * <ul>
 *   <li>通用基础实体(预留多租户 BaseTenant、创建人审计等)</li>
 *   <li>模块间 Facade(例如 InventoryChangeFacade, mall 依赖、erp/wms 提供实现)</li>
 *   <li>erupt 通用 DataProxy / ChoiceHandler 模板</li>
 * </ul>
 */
package xyz.herz.ep.common;
