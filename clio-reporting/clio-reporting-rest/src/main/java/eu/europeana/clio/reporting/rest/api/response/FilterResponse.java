package eu.europeana.clio.reporting.rest.api.response;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import eu.europeana.clio.common.model.CheckRecord;
import eu.europeana.clio.common.model.FieldFilters;
import eu.europeana.clio.common.model.FieldNames;
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
public class FilterResponse {

  private List<CheckRecord> results;
  @JsonInclude(JsonInclude.Include.NON_NULL)
  @JsonProperty(FieldNames.FILTERS)
  private FieldFilters filterOptions;

}
