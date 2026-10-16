package com.ziyi.leetcodereviewsystem.coach;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.*;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import java.util.*;

@Service
public class CoachCatalogInitializer {
    private final CoachStore db;
    public CoachCatalogInitializer(CoachStore db){this.db=db;}
    @Transactional public void seed() {
        // Independently ordered, project-authored foundation path; not a NeetCode list.
        String[][] entries={
          {"1","Two Sum","two-sum","Easy","hash-table","Hash Table","Given numbers and a target, reason about whether a pair of distinct positions can meet the target. Track which facts about earlier positions could help with one current number. Consider duplicates and whether one position can be reused."},
          {"704","Binary Search","binary-search","Easy","binary-search","Binary Search","In an ordered sequence, find a requested value or establish that it is absent. Reason about a shrinking region that may still contain it. Pay attention to empty input, interval boundaries and guaranteed progress."},
          {"125","Valid Palindrome","valid-palindrome","Easy","two-pointers","Two Pointers","Consider a sequence after ignoring non-alphanumeric characters and case. Determine whether its forward and reverse readings agree. Clarify empty input and the character set before discussing how to compare positions."},
          {"70","Climbing Stairs","climbing-stairs","Easy","dynamic-programming","Dynamic Programming","Count ways of reaching a step when each move advances one or two steps. Small cases reveal what information a later count depends on. Discuss base cases before generalizing."},
          {"78","Subsets","subsets","Medium","backtracking","Backtracking","Enumerate selections from distinct elements, including the empty selection. Think about what one decision adds and how to avoid listing one selection twice. This is project-authored conceptual context, not a problem statement."},
          {"217","Contains Duplicate","contains-duplicate","Easy","hash-table","Hash Table","Determine whether any two positions contain equal values. Compare information needed for membership against information needed for counting. Think about repeated values without relying on their numeric sign."},
          {"15","3Sum","3sum","Medium","two-pointers","Two Pointers","Find distinct value triples meeting a zero-sum constraint. Begin by clarifying distinct values versus distinct positions and repeated inputs. Explore one small sorted example before reasoning about movement or duplicate handling."},
          {"49","Group Anagrams","group-anagrams","Medium","hash-table","Hash Table","Partition words by whether they contain the same multiset of characters. Ask what representation remains unchanged when characters are reordered. Distinguish character membership from multiplicity."},
          {"200","Number of Islands","number-of-islands","Medium","dfs","DFS","Explore connected groups of occupied cells in a rectangular grid with orthogonal adjacency. Distinguish visiting one cell from discovering one component. Discuss bounds, visited state and disconnected regions."}
        };
        int position=0;
        for(String[] e:entries){
            var existing=db.rows("select id from problems where platform='LEETCODE' and external_problem_id=?",e[0]);
            int id;
            if(existing.isEmpty()) {
                id=((Number)db.one("insert into problems(title,difficulty,pattern,times_reviewed,solved,platform,external_problem_id,slug,url) values (?,?,?,0,false,'LEETCODE',?,?,?) returning id",e[1],e[3],e[5],e[0],e[2],"https://leetcode.com/problems/"+e[2]+"/").get("id")).intValue();
            } else id=((Number)existing.getFirst().get("id")).intValue();
            int pattern=((Number)db.one("insert into patterns(slug,name) values (?,?) on conflict(slug) do update set slug=excluded.slug returning id",e[4],e[5]).get("id")).intValue();
            db.update("insert into problem_patterns(problem_id,pattern_id) values (?,?) on conflict do nothing",id,pattern);
            db.update("insert into coach_catalog(id,context_version,teaching_context,provenance,position) values (?,'original-v1',?,?,?) on conflict(id) do nothing",id,e[6],"Official factual identifier/link: https://leetcode.com/problems/"+e[2]+"/; independently authored teaching context and ordering, project MIT license; no proprietary statements or solutions",position++);
        }
    }
    @Configuration @Profile({"coach","staging"}) static class Startup {
        @Bean ApplicationRunner coachCatalogSeed(CoachCatalogInitializer catalog){return args->catalog.seed();}
    }
}
