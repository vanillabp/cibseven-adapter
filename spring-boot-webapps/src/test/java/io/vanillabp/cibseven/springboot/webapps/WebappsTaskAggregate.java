package io.vanillabp.cibseven.springboot.webapps;

import io.vanillabp.spi.service.NoSyncWithBPMS;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * The workflow aggregate of the process whose user task a web application completes.
 */
@Entity
@Table(name = "WEBAPPS_TASK_AGGREGATE")
@Getter
@Setter
public class WebappsTaskAggregate {

  @Id
  private String id;

  /** The id of the user task, set when VanillaBP is told about the task. */
  @NoSyncWithBPMS
  private String reviewTaskId;

  /** Set by the service task after the user task, so only after the completion. */
  @NoSyncWithBPMS
  private boolean archived;

}
