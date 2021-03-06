/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kodality.blaze.search.sql.params;

import com.kodality.blaze.core.exception.FhirException;
import com.kodality.blaze.core.model.search.QueryParam;
import com.kodality.blaze.core.util.DateUtil;
import com.kodality.blaze.search.sql.SearchPrefix;
import com.kodality.blaze.util.sql.SqlBuilder;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.OperationOutcome.IssueType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DateExpressionProvider extends ExpressionProvider {
  private static final Map<Integer, String> intervals;
  private static final String[] operators =
      { null, SearchPrefix.le, SearchPrefix.lt, SearchPrefix.ge, SearchPrefix.gt };

  static {
    intervals = new HashMap<>();
    intervals.put(1, "1 year");
    intervals.put(2, "1 month");
    intervals.put(3, "1 day");
    intervals.put(4, "1 hour");
    intervals.put(5, "1 minute");
    intervals.put(6, "1 second");
    intervals.put(7, "1 second");
    intervals.put(8, "1 second");
  }

  @Override
  public SqlBuilder makeExpression(QueryParam param, String alias) {
    List<SqlBuilder> ors = new ArrayList<>();
    for (String value : param.getValues()) {
      if (!StringUtils.isEmpty(value)) {
        SqlBuilder sb = new SqlBuilder("EXISTS (SELECT 1 FROM " + parasol(param, alias));
        sb.and(rangeSql("range", value)).append(")");
        ors.add(sb);
      }
    }
    return new SqlBuilder().or(ors);
  }

  public static SqlBuilder makeExpression(String field, QueryParam param) {
    List<SqlBuilder> ors = new ArrayList<>();
    for (String value : param.getValues()) {
      if (!StringUtils.isEmpty(value)) {
        ors.add(new SqlBuilder(rangeSql(field, value)));
      }
    }
    return new SqlBuilder().or(ors);
  }

  @Override
  public SqlBuilder order(String resourceType, String key, String alias) {
    String sql = String.format("SELECT range FROM " + parasol(resourceType, key, alias), alias);
    return new SqlBuilder("(" + sql + ")");
  }

  private static String rangeSql(String field, String value) {
    SearchPrefix prefix = SearchPrefix.parse(value, operators);
    String search = range(prefix.getValue());
    if (prefix.getPrefix() == null) {
      return field + " && " + search;
    }
    if (prefix.getPrefix().equals(SearchPrefix.lt)) {
      return field + " << " + search;
    }
    if (prefix.getPrefix().equals(SearchPrefix.gt)) {
      return field + " >> " + search;
    }
    if (prefix.getPrefix().equals(SearchPrefix.le)) {
      return "(" + field + " && " + search + " OR " + field + " << " + search + ")";
    }
    if (prefix.getPrefix().equals(SearchPrefix.ge)) {
      return "(" + field + " && " + search + " OR " + field + " >> " + search + ")";
    }

    throw new FhirException(400, IssueType.INVALID, "prefix " + prefix.getPrefix() + " not supported");
  }

  private static String range(String value) {
    value = StringUtils.replace(value, "Z", "+00:00");
    String[] input = StringUtils.split(value, "-T:+");
    String interval = intervals.get(input.length);
    String[] mask = mask(input);
    String date = String.format("%s-%s-%sT%s:%s:%s+%s:%s", (Object[]) mask);
    DateUtil.parse(date, DateUtil.ISO_DATETIME).orElseThrow(() -> new IllegalArgumentException("Cannot parse date " + date)); //just for validation
    return "range('" + date + "', '" + interval + "')";
  }

  private static String[] mask(String[] input) {
    String[] mask = new String[] { "0000", "01", "01", "00", "00", "00", "00", "00" };
    System.arraycopy(input, 0, mask, 0, input.length);
    return mask;
  }

}
