package dsaNexsus;

import java.util.*;

//Relief Request Class

class ReliefRequest {

 String lID, lName, resType;

 int priority, quantity;

 long timestamp;



 public ReliefRequest(String lID, String lName, String resType, int priority, int quantity) {

     this.lID = lID;

     this.lName = lName;

     this.resType = resType;

     this.priority = priority;

     this.quantity = quantity;

     this.timestamp = System.currentTimeMillis();

 }

}



//Priority Queue using Min-Heap

class PriorityQ {

 ReliefRequest[] heap;

 int size;



 PriorityQ(int capacity) {

     heap = new ReliefRequest[capacity];

     size = 0;

 }



 private void swap(int i, int j) {

     ReliefRequest temp = heap[i];

     heap[i] = heap[j];

     heap[j] = temp;

 }



 private boolean isHigherPriority(ReliefRequest a, ReliefRequest b) {

     return a.priority < b.priority || (a.priority == b.priority && a.timestamp < b.timestamp);

 }



 public void insert(ReliefRequest request) {

     heap[size] = request;

     int current = size++;

     while (current > 0 && isHigherPriority(heap[current], heap[(current - 1) / 2])) {

         swap(current, (current - 1) / 2);

         current = (current - 1) / 2;

     }

 }



 public ReliefRequest extractMin() {

     if (size == 0) return null;

     ReliefRequest result = heap[0];

     heap[0] = heap[--size];

     int current = 0;

     while (true) {

         int left = 2 * current + 1, right = 2 * current + 2, smallest = current;

         if (left < size && isHigherPriority(heap[left], heap[smallest])) smallest = left;

         if (right < size && isHigherPriority(heap[right], heap[smallest])) smallest = right;

         if (smallest != current) {

             swap(current, smallest);

             current = smallest;

         } else break;

     }

     return result;

 }



 public ReliefRequest getOldestPending() {

     long oldest = Long.MAX_VALUE;

     ReliefRequest res = null;

     for (int i = 0; i < size; i++) {

         if (heap[i].timestamp < oldest) {

             oldest = heap[i].timestamp;

             res = heap[i];

         }

     }

     return res;

 }



 public void promoteOldRequests(long maxWaitMillis) {

     long now = System.currentTimeMillis();

     for (int i = 0; i < size; i++) {

         if ((now - heap[i].timestamp) >= maxWaitMillis && heap[i].priority > 1) {

             heap[i].priority--;

         }

     }

 }



 public boolean isEmpty() {

     return size == 0;

 }



 public List<ReliefRequest> getTopK(int k) {

     List<ReliefRequest> top = new ArrayList<>();

     PriorityQ temp = new PriorityQ(size);

     for (int i = 0; i < size; i++) temp.insert(heap[i]);

     for (int i = 0; i < k && !temp.isEmpty(); i++) top.add(temp.extractMin());

     return top;

 }

}



//Stack with Undo Limit

class Stack {

 ReliefRequest[] stack;

 int top;

 final int UNDO_LIMIT = 3;



 Stack(int capacity) {

     stack = new ReliefRequest[capacity];

     top = -1;

 }



 public void push(ReliefRequest request) {

     if (top + 1 == UNDO_LIMIT) pop();

     stack[++top] = request;

 }



 public ReliefRequest pop() {

     return top == -1 ? null : stack[top--];

 }



 public boolean isEmpty() {

     return top == -1;

 }



 public List<ReliefRequest> getHistory() {

     List<ReliefRequest> history = new ArrayList<>();

     for (int i = 0; i <= top; i++) history.add(stack[i]);

     return history;

 }

}



//Custom Hash Map

class MyHashMap {

 class Node {

     String key;

     ReliefRequest value;

     Node next;



     Node(String key, ReliefRequest value) {

         this.key = key;

         this.value = value;

     }

 }



 Node[] buckets;

 int size;



 public MyHashMap(int size) {

     this.size = size;

     buckets = new Node[size];

 }



 private int getIndex(String key) {

     int hash = 0;

     for (char ch : key.toCharArray()) hash = (hash * 31 + ch) % size;

     return hash;

 }



 public void put(String key, ReliefRequest value) {

     int index = getIndex(key);

     Node head = buckets[index];

     while (head != null) {

         if (head.key.equals(key)) {

             head.value = value;

             return;

         }

         head = head.next;

     }

     Node newNode = new Node(key, value);

     newNode.next = buckets[index];

     buckets[index] = newNode;

 }



 public ReliefRequest get(String key) {

     int index = getIndex(key);

     Node head = buckets[index];

     while (head != null) {

         if (head.key.equals(key)) return head.value;

         head = head.next;

     }

     return null;

 }



 public void remove(String key) {

     int index = getIndex(key);

     Node head = buckets[index], prev = null;

     while (head != null) {

         if (head.key.equals(key)) {

             if (prev == null) buckets[index] = head.next;

             else prev.next = head.next;

             return;

         }

         prev = head;

         head = head.next;

     }

 }



 public List<ReliefRequest> getAll() {

     List<ReliefRequest> all = new ArrayList<>();

     for (Node bucket : buckets) {

         while (bucket != null) {

             all.add(bucket.value);

             bucket = bucket.next;

         }

     }

     return all;

 }



 public int countRequestsByType(String type) {

     int count = 0;

     for (Node bucket : buckets) {

         while (bucket != null) {

             if (bucket.value.resType.equalsIgnoreCase(type)) count++;

             bucket = bucket.next;

         }

     }

     return count;

 }

}



//Main Allocator System

class DisasterReliefAllocator {

 PriorityQ queue = new PriorityQ(100);

 Stack stack = new Stack(100);

 MyHashMap map = new MyHashMap(50);

 Map<String, Integer> resourceStock = new HashMap<>();

 int dispatchedCount = 0;



 public DisasterReliefAllocator() {

     resourceStock.put("Food", 50);

     resourceStock.put("Water", 50);

     resourceStock.put("Medical", 50);

 }



 public void addRequest(ReliefRequest request) {

     queue.insert(request);

     map.put(request.lID, request);

     //stack.push(request);

 }



 public void allocate() {

     queue.promoteOldRequests(60000); // auto-promote after 60 seconds

     ReliefRequest r = queue.extractMin();

     if (r != null && resourceStock.getOrDefault(r.resType, 0) >= r.quantity) {

         System.out.println("\u001B[32mDispatched to: " + r.lName + " | Resource: " + r.resType + " | Qty: " + r.quantity + "\u001B[0m");

         dispatchedCount++;

         resourceStock.put(r.resType, resourceStock.get(r.resType) - r.quantity);

         map.remove(r.lID);

         stack.push(r);

     } else if (r != null) {

         System.out.println("\u001B[31mInsufficient resource for: " + r.resType + "\nNo requests could be fulfilled. Consider refilling stock.\u001B[0m");

         queue.insert(r); // reinsert the request

     } else {

         System.out.println("No pending requests.");

     }

 }



 public void undo() {

     ReliefRequest last = stack.pop();

     if (last != null) {

         System.out.println("Undoing for: " + last.lName);

         queue.insert(last);

         map.put(last.lID, last);

         resourceStock.put(last.resType, resourceStock.getOrDefault(last.resType, 0) + last.quantity);

     } else {

         System.out.println("Nothing to undo.");

     }

 }



 public void showRequest(String locationID) {

     ReliefRequest r = map.get(locationID);

     if (r != null) {

         System.out.println("Location: " + r.lName + " | Resource: " + r.resType + " | Priority: " + r.priority);

     } else {

         System.out.println("No request found for: " + locationID);

     }

 }



 public void showTopUrgent(int k) {

     List<ReliefRequest> urgent = queue.getTopK(k);

     System.out.println("\nTop " + k + " Urgent Requests:");

     for (ReliefRequest r : urgent) {

         System.out.println("- " + r.lName + " | " + r.resType + " | Priority: " + r.priority);

     }

 }



 public void showStats() {

     List<ReliefRequest> pending = map.getAll();

     Map<String, Integer> resCount = new HashMap<>();

     for (ReliefRequest r : pending) {

         resCount.put(r.resType, resCount.getOrDefault(r.resType, 0) + 1);

     }



     System.out.println("\n--- Request Statistics ---");

     System.out.println("Total Requests: " + (pending.size() + dispatchedCount));

     System.out.println("Dispatched: " + dispatchedCount);

     System.out.println("Pending: " + pending.size());

     System.out.println("Most Requested Resource: " +

         resCount.entrySet().stream().max(Map.Entry.comparingByValue()).orElse(Map.entry("None", 0)).getKey());

     System.out.println("Available Resources: " + resourceStock);

 }



 public void showAnalytics() {

     System.out.println("\nResource Request Count:");

     for (String type : new String[]{"Medical", "Food", "Water"}) {

         System.out.println(type + ": " + map.countRequestsByType(type));

     }

     ReliefRequest oldest = queue.getOldestPending();

     if (oldest != null) {

         System.out.println("Oldest Pending: " + oldest.lName + " - " + oldest.resType);

     }

 }

 

 public void refillResource(String type, int quantity) {

     int current = resourceStock.getOrDefault(type, 0);

     resourceStock.put(type, current + quantity);

     System.out.println("Resource stock updated: " + type + " = " + (current + quantity));

 }

}



//Main Class

public class Main {

 public static void main(String[] args) {

     DisasterReliefAllocator allocator = new DisasterReliefAllocator();

     Scanner sc = new Scanner(System.in);



     while (true) {

         System.out.println("\n=== Disaster Relief Resource Allocator ===");

         System.out.println("1. Add Relief Request");

         System.out.println("2. Allocate Resource (Dispatch)");

         System.out.println("3. Undo Last Dispatch");

         System.out.println("4. Show Request by Location ID");

         System.out.println("5. View Top 3 Urgent Requests");

         System.out.println("6. View Statistics");

         System.out.println("7. Show System Analytics");

         System.out.println("8. Refill Resource Stock");

         System.out.println("9. Exit");

         System.out.print("Enter your choice: ");

         int choice = sc.nextInt();

         sc.nextLine();



         switch (choice) {

             case 1:

                 System.out.print("Enter Location ID: ");

                 String locID = sc.nextLine();

                 System.out.print("Enter Location Name: ");

                 String locName = sc.nextLine();

                 System.out.print("Enter Resource Type (Food/Water/Medical): ");

                 String resType = sc.nextLine();

                 System.out.print("Enter Quantity: ");

                 int qty = sc.nextInt();

                 System.out.print("Enter Priority (1-5): ");

                 int prio = sc.nextInt();

                 allocator.addRequest(new ReliefRequest(locID, locName, resType, prio, qty));

                 break;

             case 2:

                 allocator.allocate();

                 break;

             case 3:

                 allocator.undo();

                 break;

             case 4:

                 System.out.print("Enter Location ID to search: ");

                 allocator.showRequest(sc.nextLine());

                 break;

             case 5:

                 allocator.showTopUrgent(3);

                 break;

             case 6:

                 allocator.showStats();

                 break;

             case 7:

                 allocator.showAnalytics();

                 break;

             case 8:

             	System.out.print("Enter Resource Type to Refill: ");

                 String refillType = sc.next();

                 System.out.print("Enter Quantity to Add: ");

                 int addQty = sc.nextInt();

                 allocator.refillResource(refillType, addQty);

                 break;

             case 9:

                 System.out.println("Exiting system. Stay safe!");

                 return;

             default:

                 System.out.println("Invalid choice.");

         }

     }

 }

}
