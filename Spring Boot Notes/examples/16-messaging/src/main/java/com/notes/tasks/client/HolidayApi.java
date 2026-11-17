package com.notes.tasks.client;

import com.notes.tasks.client.HolidayClient.Holiday;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.List;

/** HTTP interface: declare the remote API as a Java interface; Spring generates the implementation. */
@HttpExchange("/holidays")
public interface HolidayApi {

    @GetExchange("/{year}")
    List<Holiday> holidays(@PathVariable int year);
}
