import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class PositionalIndex {
    private static Map<String, DictEntry> index = new HashMap<>();
    private static List<String> documents = new ArrayList<>();
    private static int documentCount = 10;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        for (int i = 1; i <= documentCount; i++) {
            File file = new File("file" + i);
            documents.add("file" + i);
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
                String line;
                int position = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] words = line.split("\\W+");
                    position++;
                    for (String word : words) {
                        addPosting(word, i,position);
                        position++;
                    }
                }
                bufferedReader.close();
            } catch (IOException e) {
                System.out.println("Error reading " + file + " -> " + i);
            }
        }

        System.out.println("1- Search for a term in documents.");
        System.out.println("2- calculate cosine similarity for a query.");
        System.out.println("3- search phrase.");
        System.out.println("4- web crawler");
        System.out.println("5- calculate tf-idf for each document.");
        int choice = sc.nextInt();
        sc.nextLine();
        if (choice == 1) {
            System.out.print("Search for: ");
            String word = sc.next();
            findWord(word);
        } else if (choice == 2) {
            System.out.print("Enter a query:");
            String query = sc.nextLine();
            calculateCosineSimilarity(query);
        }
        else if(choice==3){
            System.out.print("Enter a query:");
            String query = sc.nextLine();
            searchPhrase(query);

        }
        else if(choice==4){   //https://example.com/
            System.out.println("Enter the starting URL for web crawling:");
            String startingURL = sc.nextLine();
            int depthLimit = 2;
            getPageLinks(startingURL, depthLimit);

            // Print the positional index
            for (Map.Entry<String, List<Integer>> entry : positionalIndex.entrySet()) {
                String word = entry.getKey();
                List<Integer> positions = entry.getValue();
                System.out.println("Word: " + word + ", Positions: " + positions);
            }
        }
        else if(choice==5){
            printTFIDFMatrix();
        }
        else {
            System.out.println("Invalid choice. Exiting program.");
        }
    }
    private static void printTFIDFMatrix() {
        System.out.println("TF-IDF Matrix:");
        System.out.print("Terms\t\t");
        for (String document : documents) {
            System.out.print("\t"+ document);
        }
        System.out.println();
        System.out.println("-----------------------------------------------------------------------------------------------");

        for (Map.Entry<String, DictEntry> entry : index.entrySet()) {
            String word = entry.getKey();
            DictEntry dictEntry = entry.getValue();

            System.out.printf("%-12s\t", word);
            for (int i = 1; i <= documentCount; i++) {
                double tfidf = getTFIDF(word, i);
                System.out.printf("%.3f\t", tfidf);
            }
            System.out.println();
        }
    }

    private static double getTFIDF(String word, int docId) {
        if (index.containsKey(word)) {
            DictEntry entry = index.get(word);
            Posting p = entry.pList;
            while (p != null) {
                if (p.docId == docId) {
                    return p.tfidf;
                }
                p = p.next;
            }
        }
        return 0.0;
    }

    public static void findWord(String word) {
        if (index.containsKey(word)) {
            DictEntry entry = index.get(word);
            Posting p = entry.pList;
            System.out.println("(" + word + ") appears in the following " + entry.df + " files: ");
            while (p != null) {
                System.out.println("File " + documents.get(p.docId - 1) + " - " + p.positions.size() + " times at positions: " + p.positions);
                p = p.next;
            }
        } else {
            System.out.println("The word " + word + " is not in the index.");
        }
    }

    private static void addPosting(String word, int docId, int position) {
        if (!index.containsKey(word)) {
            index.put(word, new DictEntry());
        }
        DictEntry entry = index.get(word);
        Posting p = entry.pList;
        boolean flag = false;

        while (p != null) {
            if (p.docId == docId) {
                flag = true;
                p.tf++ ;
                p.positions.add(position);
                break;
            }
            p = p.next;
        }

        if (!flag) {
            Posting posting = new Posting();
            posting.docId = docId;
            posting.positions = new ArrayList<>();
            posting.positions.add(position);
            posting.tf = 1;

            if (entry.pList == null) {
                entry.pList = posting;
            } else {
                p = entry.pList;
                while (p.next != null) {
                    p = p.next;
                }
                p.next = posting;
            }
            entry.df++;
        }

        // Update the tf-idf value
      updateTFIDF(word);
    }
    private static void updateTFIDF(String word) {
        DictEntry entry = index.get(word);
        double idf = Math.log10((double) documentCount / entry.df); // Calculate IDF

        Posting p = entry.pList;
        while (p != null) {
            double tfidf = p.tf * idf; // Calculate TF-IDF
            p.tfidf = tfidf;
            p = p.next;
        }
    }

    public static void searchPhrase(String query) {
        query = query.toLowerCase();
        // Split the query into individual words
        String[] words = query.split("\\s+");

        // Retrieve the DictEntry for the first word
        DictEntry firstEntry = index.get(words[0]);

        // If the first word doesn't exist or has an empty posting list, no documents are found
        if (firstEntry == null || firstEntry.pList == null) {
            System.out.println("No documents found.");
            return;
        }

        // Iterate over the posting list of the first word
        Posting p = firstEntry.pList;
        boolean flag = false;
        while (p != null) {
            // Initialize a boolean flag to track if the phrase is found in the document
            boolean phraseFound = false;

            // Get the list of positions for the first word
            List<Integer> positions = p.positions;

            // Iterate over the positions of the first word
            for (int position : positions) {
                // Initialize the current position
                int currentPosition = position;

                // Initialize a boolean flag to track if the phrase is found at the current position
                boolean found = true;

                // Iterate over the remaining words in the query
                for (int i = 1; i < words.length; i++) {
                    // Retrieve the DictEntry for the current word
                    DictEntry entry = index.get(words[i]);

                    // If the word doesn't exist or has an empty posting list, the phrase is not found
                    if (entry == null || entry.pList == null) {
                        found = false;
                        break;
                    }

                    // Iterate over the posting list of the current word
                    Posting nextPosting = entry.pList;
                    boolean wordFound = false;

                    // Check if the next posting belongs to the same document as the current posting
                    while (nextPosting != null) {
                        if (nextPosting.docId == p.docId) {
                            // Get the positions for the current word in the next posting
                            List<Integer> nextPositions = nextPosting.positions;

                            // Check if the position of the current word is present
                            // at the appropriate position relative to the first word
                            for (int nextPosition : nextPositions) {
                                if (nextPosition == currentPosition + i) {
                                    wordFound = true;
                                    break;
                                }
                            }
                            break;
                        }
                        nextPosting = nextPosting.next;
                    }

                    // If the current word is not found at the appropriate position, the phrase is not found
                    if (!wordFound) {
                        found = false;
                        break;
                    }
                }

                // If the phrase is found, set the flag to true
                if (found) {
                    phraseFound = true;
                    break;
                }
            }

            // If the phrase is found in the document, print the document identifier
            if(!flag && phraseFound){
                System.out.println("Documents containing \"" + query + "\":");
                flag = true;
                System.out.println("Document " + p.docId);
            } else if (phraseFound) {
                flag = true;
                System.out.println("Document " + p.docId);
            }
            // Move to the next posting in the list
            p = p.next;
        }
        if(!flag){
            System.out.println("No documents found.");
        }
    }

    public static void calculateCosineSimilarity(String query) {
        List<Map.Entry<String, Double>> cosineSimilarityList = new ArrayList<>();
        String[] queryWords = query.split("\\W+");
        Map<String, Double> queryTfIdf = new HashMap<>();

        // Calculate TF-IDF for query words
        for (String word : queryWords) {
            double tf = calculateTermFrequency(queryWords, word);
//            System.out.println("words "+word);
            queryTfIdf.put(word, tf);
        }
        // Calculate cosine similarity for each document
        for (int i = 1; i <= 10; i++) {
            double dotProduct = 0;
            double docMagnitude = 0;
            double queryMagnitude = 0;

            for (String word : queryTfIdf.keySet()) {
                if (!index.containsKey(word)) {
                    continue; // Skip if word not found in index
                }
                DictEntry entry = index.get(word);
                Posting p = entry.pList;

                while (p != null) {
                    if (p.docId == i) {
                        double docTfIdf = p.tf;
                        dotProduct += queryTfIdf.get(word) * docTfIdf;

                        break;
                    }
                    p = p.next;
                }
                double queryTfId = queryTfIdf.get(word);
                queryMagnitude += queryTfId * queryTfId;
            }
            // Calculate document magnitude
            for (DictEntry entry : index.values()) {
                Posting p = entry.pList;
                while (p != null) {
                    if (p.docId == i) {
                        double docTfIdf = p.tf;
                        docMagnitude += docTfIdf * docTfIdf;
                    }
                    p = p.next;
                }
            }
            double cosineSimilarity = dotProduct / (Math.sqrt(docMagnitude) * Math.sqrt(queryMagnitude));
            cosineSimilarityList.add(new AbstractMap.SimpleEntry<>(documents.get(i - 1), cosineSimilarity));
            // Sort the list based on cosine similarity (in descending order)
            Collections.sort(cosineSimilarityList, (entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

            // Print the sorted list

//            System.out.println("Cosine similarity with File " + documents.get(i - 1) + ": " + cosineSimilarity);
        }
        for (Map.Entry<String, Double> entry : cosineSimilarityList) {
            System.out.println("Cosine similarity with File " + entry.getKey() + ": " + entry.getValue());
        }

    }


    private static double calculateTermFrequency(String[] words, String word) {
        int count = 0;
        for (String w : words) {
            if (w.equalsIgnoreCase(word)) {
                count++;
            }
        }
        return count;
    }
    private static List<String> links = new ArrayList<>();
    private static Map<String, List<Integer>> positionalIndex = new HashMap<>();

    public static void getPageLinks(String URL, int depth) {
        if (depth <= 0 || links.contains(URL)) {
            return; // Terminate if depth limit reached or URL already visited
        }
        try {
            if (links.add(URL)) {
                System.out.println(URL);
            }
            Document document = Jsoup.connect(URL).get();
            String content = document.text();
            String[] words = content.split("\\W+");

            for (int i = 0; i < words.length; i++) {
                String word = words[i].toLowerCase();

                if (!positionalIndex.containsKey(word)) {
                    positionalIndex.put(word, new ArrayList<>());
                }
                positionalIndex.get(word).add(i);
            }

            Elements linksOnPage = document.select("a[href]");

            for (Element page : linksOnPage) {
                getPageLinks(page.attr("abs:href"), depth - 1);
            }
        } catch (IOException e) {
            System.err.println("For '" + URL + "': " + e.getMessage());
        }
    }





}
