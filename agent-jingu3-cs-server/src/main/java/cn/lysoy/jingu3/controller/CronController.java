package cn.lysoy.jingu3.controller;

import cn.lysoy.jingu3.common.api.ApiResult;
import cn.lysoy.jingu3.cron.dto.CreateScheduledTaskRequest;
import cn.lysoy.jingu3.cron.dto.ScheduledTaskVo;
import cn.lysoy.jingu3.cron.ScheduledTaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Cron MVP：创建与查询定时任务；到期执行由 {@link cn.lysoy.jingu3.cron.ScheduledTaskPoller} 驱动。
 */
@RestController
@RequestMapping("/api/v1/cron")
public class CronController {

    private final ScheduledTaskService scheduledTaskService;

    public CronController(ScheduledTaskService scheduledTaskService) {
        this.scheduledTaskService = scheduledTaskService;
    }

    @PostMapping("/tasks")
    public ApiResult<ScheduledTaskVo> create(@Valid @RequestBody CreateScheduledTaskRequest request) {
        return ApiResult.ok(scheduledTaskService.create(request));
    }

    @GetMapping("/tasks")
    public ApiResult<List<ScheduledTaskVo>> list() {
        return ApiResult.ok(scheduledTaskService.listMine());
    }
}
