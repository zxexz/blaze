package com.nortal.blaze.search.sql;

import com.nortal.blaze.core.exception.ServerException;
import com.nortal.blaze.core.model.search.QueryParam;
import com.nortal.blaze.core.service.conformance.ConformanceHolder;
import com.nortal.blaze.search.sql.params.*;
import com.nortal.blaze.util.sql.SqlBuilder;
import org.hl7.fhir.dstu3.model.Enumerations.SearchParamType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SqlToster {
  private static final Map<String, SpecialParamBuilder> specialParams;
  private static final Map<SearchParamType, ExpressionProvider> providers;

  static {
    specialParams = new HashMap<>();
    specialParams.put("_id", (p, a) -> new SqlBuilder().in(a + ".id", p.getValues()));
    specialParams.put("_lastUpdated",
                      (p, a) -> DateExpressionProvider.makeExpression("range_instant(" + a + ".last_updated)", p));

    providers = new HashMap<SearchParamType, ExpressionProvider>();
    providers.put(SearchParamType.STRING, new StringExpressionProvider());
    providers.put(SearchParamType.TOKEN, new TokenExpressionProvider());
    providers.put(SearchParamType.DATE, new DateExpressionProvider());
    providers.put(SearchParamType.REFERENCE, new ReferenceExpressionProvider());
    providers.put(SearchParamType.NUMBER, new NumberExpressionProvider());
  }

  private SqlToster() {
    //
  }

  public static SqlBuilder chain(List<QueryParam> params, String alias) {
    return ReferenceExpressionProvider.chain(params, alias);
  }

  public static SqlBuilder condition(QueryParam param, String alias) {
    String key = param.getKey();
    if (specialParams.containsKey(key)) {
      return specialParams.get(key).build(param, alias);
    }
    if (!providers.containsKey(param.getType())) {
      throw new ServerException("'" + param.getType() + "' search parameter type not implemented");
    }
    return providers.get(param.getType()).makeExpression(param, alias);
  }

  public static SqlBuilder order(QueryParam param, String alias) {
    String value = param.getValues().get(0);
    SearchParamType type = ConformanceHolder.requireSearchParam(param.getResourceType(), value).getType();
    // String key = param.getKey();
    // if (specialParams.containsKey(key)) {
    // return specialParams.get(key).build(param, alias);
    // }
    if (!providers.containsKey(type)) {
      throw new ServerException("'" + param.getType() + "' search parameter type not implemented");
    }
    boolean isDesc = "desc".equals(param.getModifier());
    return providers.get(type).order(param.getResourceType(), value, alias).append(isDesc ? " DESC" : " ASC");
  }

  private interface SpecialParamBuilder {
    SqlBuilder build(QueryParam param, String alias);
  }
}
