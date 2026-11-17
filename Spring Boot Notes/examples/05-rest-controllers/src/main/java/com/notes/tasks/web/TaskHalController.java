package com.notes.tasks.web;

import com.notes.tasks.dto.TaskDtos.TaskResponse;
import com.notes.tasks.model.Task;
import com.notes.tasks.service.TaskService;
import org.springframework.hateoas.EntityModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/** HATEOAS: the response also tells the client what it can do next, as links. */
@RestController
@RequestMapping("/api/hal/tasks")
public class TaskHalController {
    private final TaskService service;

    public TaskHalController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public EntityModel<TaskResponse> get(@PathVariable long id) {
        Task t = service.get(id);
        EntityModel<TaskResponse> model = EntityModel.of(TaskResponse.from(t));
        model.add(linkTo(methodOn(TaskHalController.class).get(id)).withSelfRel());
        model.add(linkTo(methodOn(TaskController.class).list(null, null)).withRel("all-tasks"));
        if (!t.isDone()) {                                   // links depend on the state
            // NOT methodOn(...).patch(...): patch() returns a record (final class), which
            // methodOn cannot proxy -> 500. Build the URL from the controller class instead.
            model.add(linkTo(TaskController.class).slash(id).withRel("complete"));
        }
        return model;
    }
}
