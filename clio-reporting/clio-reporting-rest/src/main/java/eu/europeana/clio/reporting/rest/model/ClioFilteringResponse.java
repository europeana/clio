package eu.europeana.clio.reporting.rest.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europeana.clio.common.model.CheckRecord;
import eu.europeana.clio.common.model.ClioFilters;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Class that saves the available filtering options and the filtering results
 */
@JsonSerialize
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ClioFilteringResponse {

  private List<CheckRecord> results;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private ClioFilters filteringOptions;

}

