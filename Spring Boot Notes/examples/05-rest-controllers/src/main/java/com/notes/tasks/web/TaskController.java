package com.notes.tasks.web;

import com.notes.tasks.dto.TaskDtos.AttachmentInfo;
import com.notes.tasks.dto.TaskDtos.CreateTaskRequest;
import com.notes.tasks.dto.TaskDtos.PatchTaskRequest;
import com.notes.tasks.dto.TaskDtos.ReplaceTaskRequest;
import com.notes.tasks.dto.TaskDtos.TaskResponse;
import com.notes.tasks.dto.TaskDtos.TaskResponseV2;
import com.notes.tasks.model.Priority;
import com.notes.tasks.model.Task;
import com.notes.tasks.service.TaskService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController                         // = @Controller + @ResponseBody on every method
@RequestMapping("/api/tasks")           // common prefix for every method below
public class TaskController {
    private final TaskService service;
    private final RequestInfo requestInfo;              // request-scoped bean (a proxy)

    public TaskController(TaskService service, RequestInfo requestInfo) {
        this.service = service;
        this.requestInfo = requestInfo;
    }

    // GET /api/tasks?done=false&priority=HIGH  (both optional)
    @GetMapping
    @AuditLog("list tasks")
    public List<TaskResponse> list(@RequestParam(required = false) Boolean done,
                                   @RequestParam(required = false) Priority priority) {
        return service.findAll(done, priority).stream().map(TaskResponse::from).toList();
    }

    // GET /api/tasks/7 with header "API-Version: 1" (or no header: 1 is the default)
    @GetMapping(path = "/{id}", version = "1")
    public TaskResponse getV1(@PathVariable long id) {
        return TaskResponse.from(service.get(id));
    }

    // GET /api/tasks/7 with header "API-Version: 2"
    @GetMapping(path = "/{id}", version = "2")
    public TaskResponseV2 getV2(@PathVariable long id) {
        return TaskResponseV2.from(service.get(id));
    }

    // POST /api/tasks  -> 201 Created + Location header + body
    @PostMapping
    @AuditLog("create task")
    public ResponseEntity<TaskResponse> create(@RequestBody CreateTaskRequest body,
                                               @RequestHeader(name = "X-Client", defaultValue = "unknown") String client) {
        Task created = service.create(body);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location)
                .header("X-Created-By", client)
                .header("X-Request-Number", String.valueOf(requestInfo.number()))
                .body(TaskResponse.from(created));
    }

    // PUT /api/tasks/7  -> replace everything
    @PutMapping("/{id}")
    public TaskResponse replace(@PathVariable long id, @RequestBody ReplaceTaskRequest body) {
        return TaskResponse.from(service.replace(id, body));
    }

    // PATCH /api/tasks/7  -> change only the fields sent
    @PatchMapping("/{id}")
    public TaskResponse patch(@PathVariable long id, @RequestBody PatchTaskRequest body) {
        return TaskResponse.from(service.patch(id, body));
    }

    // DELETE /api/tasks/7  -> 204 No Content
    @DeleteMapping("/{id}")
    @AuditLog("delete task")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    // POST /api/tasks/7/attachment  (multipart/form-data, part name "file")
    @PostMapping(path = "/{id}/attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AttachmentInfo upload(@PathVariable long id, @RequestPart("file") MultipartFile file) throws IOException {
        service.get(id);                                     // 404 if the task doesn't exist
        String text = new String(file.getBytes(), StandardCharsets.UTF_8);
        return new AttachmentInfo(id, file.getOriginalFilename(), file.getContentType(),
                file.getSize(), (int) text.lines().count());
    }
}
