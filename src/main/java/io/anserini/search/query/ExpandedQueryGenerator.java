package io.anserini.search.query;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;

import io.anserini.analysis.AnalyzerUtils;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.TermQuery;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


public class ExpandedQueryGenerator extends QueryGenerator {

  float alpha = 1;


  @Override
  public Query buildQuery(String field, Analyzer analyzer, String queryText) {
    // TODO Auto-generated method stub
    String[] queryAndExpandedTerms = queryText.split(";");
    String originalQuery =  queryAndExpandedTerms[0];
    String expandedTerms = queryAndExpandedTerms[1];

    //normal bag of terms query builder:
    List<String> tokens = AnalyzerUtils.analyze(analyzer, originalQuery);
    Map<String, Long> collect = tokens.stream()
        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    BooleanQuery.Builder builder = new BooleanQuery.Builder();
    for (String t : collect.keySet()) {
      builder.add(new BoostQuery(new TermQuery(new Term(field, t)), (float) collect.get(t)),
          BooleanClause.Occur.SHOULD);
    }
    //Add expanded terms with weights:
    String[] expandedTermsSplit = expandedTerms.split(",");
    for (String wordAndWeightString: expandedTermsSplit) {
      String[] wordAndWeight = wordAndWeightString.split(" ");
      String word = wordAndWeight[0];
      float weight = alpha * Float.parseFloat(wordAndWeight[1]);  
      List<String> tokenWord = AnalyzerUtils.analyze(analyzer, word);
      if(tokenWord.size() > 0) {
        word = tokenWord.get(0);
        builder.add(new BoostQuery(new TermQuery(new Term(field, word)), weight),
            BooleanClause.Occur.SHOULD);
      }
    }

    return builder.build();
  }

  public void setAlpha(float alpha) {
    this.alpha = alpha;
  }
  
}
