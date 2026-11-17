package com.notes.tasks.service;

import com.notes.tasks.aop.Audited;
import org.springframework.stereotype.Service;

/** Shows the proxy rule: advice runs only for calls that come through the proxy. */
@Service
public class ExportService {

    @Audited("export project")
    public String exportProject(long projectId) {
        return "project-" + projectId + ".csv";
    }

    /** Calls exportProject on "this" (the real object), not on the proxy: NO audit entries. */
    public int exportAll() {
        exportProject(1);
        exportProject(2);
        return 2;
    }
}
