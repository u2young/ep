package xyz.herz.ep.proj.jpa.task;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import xyz.herz.ep.proj.entity.task.ProjTask;

import java.util.List;

@Repository
public interface ProjTaskRepository extends JpaRepository<ProjTask, Long> {

    /** 某项目下全部任务(供 percentComplete = done/total 聚合)。 */
    List<ProjTask> findByProjectId(Long projectId);

    /** 某父任务下子任务(供树形 progress 向上回写)。 */
    List<ProjTask> findByParentTaskId(Long parentTaskId);
}
