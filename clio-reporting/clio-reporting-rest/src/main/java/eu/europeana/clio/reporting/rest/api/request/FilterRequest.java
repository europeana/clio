package eu.europeana.clio.reporting.rest.api.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tools.jackson.databind.annotation.JsonSerialize;

/**
 * Class that encapsulates all types of Clio filter, each of them with their conditions
 */
@JsonSerialize
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class FilterRequest {

  @JsonProperty(FieldNames.FILTERS)
  private FieldFilters filters;

}
