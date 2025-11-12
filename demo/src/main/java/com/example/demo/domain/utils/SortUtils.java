package com.example.demo.domain.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SortUtils {
  
  private SortUtils() {}

  public static <T> List<T> bubbleSort(List<T> items, Comparator<? super T> comparator) {
    if (items == null || items.size() <= 1) return items;

    List<T> sorted = new ArrayList<>(items);
    int size = sorted.size();

    for (int passIndex = 0; passIndex < size - 1; passIndex++) {
      boolean swapped = false;

      for (int index = 1; index < size - passIndex; index++) {
        
        if (comparator.compare(sorted.get(index - 1), sorted.get(index)) > 0) {
          T tmp = sorted.get(index - 1);
          sorted.set(index - 1, sorted.get(index));
          sorted.set(index, tmp);
          swapped = true;
        }
      }

      if (!swapped) break;
    }

    return sorted;
}
}
