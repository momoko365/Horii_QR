package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ItemDAO {
    @Insert
     fun insert(items: List<Item>) // List<Item> 型の引数を受け取る
    @Query("SELECT * FROM Item")
    fun getItemAll(): List<Item>

    // 商品点数カウント
  @Query("SELECT COUNT(DISTINCT itemCD) FROM Item")
 fun getDistinctItemCount(): Int

 //リストのケース数合計
  @Query("SELECT SUM(case_q) FROM Item")
fun getTotalCase(): Int
//リストのバラ数合計
    @Query("SELECT SUM(bara) FROM Item")
    fun getTotalBara(): Int

    // 検品ナンバーを取得
  @Query("SELECT kenpinNo FROM Item LIMIT 1")
   fun getKenpinNo(): String?

   // リストのバラ済み数合計
   @Query("SELECT SUM(barazumi) FROM Item")
    fun getBarazumi(): Int

    @Query("SELECT SUM(casezumi) FROM Item")
    fun getCasezumi(): Int


//    // suryoがzumiのアイテムの数を取得する
//    @Query("SELECT COUNT(*) FROM Item WHERE case_q = 'zumi'")
//    fun getCountOfZumiItems(): Int

//    // 全行のzumi数をカウントする
//    @Query("SELECT SUM(zumi) FROM Item")
//    fun getTotalZumiCount(): Int
//JANかITFが一致するアイテムを取得する（複数）
    @Query("SELECT * FROM ITEM WHERE JAN = :jan OR ITF = :itf")
    fun getItemByCode(jan: String, itf: String): List<Item>
//JANかITFが一致するアイテムを取得する（一つだけ）
    @Query("SELECT * FROM ITEM WHERE jan = :jan OR itf = :itf LIMIT 1")
   fun getItemCode(jan: String, itf: String): Item?

//   @Query("SELECT * FROM Item WHERE suryo - zumi > 0")
//    fun getUnfinishedItems(): List<Item>

    @Query("SELECT * FROM Item WHERE kenpinNo = :kenpinNo")
    fun getItemsByKenpinNo(kenpinNo: String): List<Item>

    @Update
    fun update(item: Item)

    @Query("SELECT count(*) FROM Item")
    fun getItemCount(): Int

    //アイテムを全削除
    @Query("DELETE FROM Item")
    fun deleteAllItems()

    //終了してるアイテム点数をカウント
//    @Query("""
//    SELECT COUNT(*)
//    FROM (
//        SELECT itemCD
//        FROM Item
//        GROUP BY itemCD
//        HAVING SUM(suryo) = SUM(zumi)
//    ) AS matched_items
//""")
//    fun getCountOfMatchedItems(): Int

    //バーコードを引数にして検品ナンバーを取得
    @Query("SELECT kenpinNo FROM Item WHERE JAN = :barcode OR ITF = :barcode")
    fun getKenpinNoByBarcode(barcode: String): String?
//
////同一検品ナンバーのアイテムの総数を取得
//    @Query("SELECT SUM(suryo) FROM Item WHERE kenpinNo = :kenpinNo")
//    fun getTotalSuryo(kenpinNo: String): Int
////同一検品ナンバーのアイテムの総数を取得
//    @Query("SELECT SUM(zumi) FROM Item WHERE kenpinNo = :kenpinNo")
//    fun getTotalZumi(kenpinNo: String): Int
//    // itemCDが一致しているもののsuryo合計を取得
//    @Query("SELECT SUM(suryo) FROM Item WHERE itemCD = (SELECT itemCD FROM Item WHERE JAN = :barcode OR ITF = :barcode LIMIT 1)")
//    fun getTotalSuryoByBarcode(barcode: String): Int
//
//    // itemCDが一致しているもののzumi合計を取得
//    @Query("SELECT SUM(zumi) FROM Item WHERE itemCD = (SELECT itemCD FROM Item WHERE JAN = :barcode OR ITF = :barcode LIMIT 1)")
//    fun getTotalZumiByBarcode(barcode: String): Int

//    //商品の入数を取り出す
//    @Query("SELECT in_q FROM Item WHERE JAN = :barcode OR ITF = :barcode LIMIT 1")
//    fun getInqByBarcode(barcode: String): Int?


}