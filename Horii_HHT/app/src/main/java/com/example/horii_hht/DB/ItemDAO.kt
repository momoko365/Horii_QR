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

//JANかITFが一致するアイテムを取得する（複数）
    @Query("SELECT * FROM ITEM WHERE JAN = :jan OR ITF = :itf")
    fun getItemByCode(jan: String, itf: String): List<Item>
//JANかITFが一致するアイテムを取得する（一つだけ）
    @Query("SELECT * FROM ITEM WHERE jan = :jan OR itf = :itf LIMIT 1")
   fun getItemCode(jan: String, itf: String): Item?

    @Query("SELECT * FROM Item WHERE kenpinNo = :kenpinNo")
    fun getItemsByKenpinNo(kenpinNo: String): List<Item>

    @Update
    fun update(item: Item)

    @Query("SELECT count(*) FROM Item")
    fun getItemCount(): Int

    //アイテムを全削除
    @Query("DELETE FROM Item")
    fun deleteAllItems()

    //バーコードを引数にして検品ナンバーを取得
    @Query("SELECT kenpinNo FROM Item WHERE JAN = :barcode OR ITF = :barcode")
    fun getKenpinNoByBarcode(barcode: String): String?


    //検品ナンバーと商品コードが一致するアイテムを取得
@Query("SELECT * FROM Item WHERE kenpinNo = :kenpinNo AND itemCD = :itemCD LIMIT 1")
fun getItemByKenpinNoAndItemCD(kenpinNo: String, itemCD: String): Item?

    //検品ナンバーと商品コードが一致するアイテムをまとめて取得
    @Query("""
        SELECT 
            SUM(case_q) as totalCaseQ, 
            SUM(bara) as totalBara, 
            SUM(casezumi) as totalCasezumi, 
            SUM(barazumi) as totalBarazumi 
        FROM Item 
        WHERE ITF = :scannedData OR JAN = :scannedData
    """)
 fun getSummarizedData(scannedData: String): SummarizedData

    @Query("""
        SELECT 
            kenpinNo, 
            itemCD, 
            itemName, 
            JAN,
            ITF,
            SUM(case_q) as totalCaseQ, 
            SUM(bara) as totalBara, 
            SUM(casezumi) as totalCasezumi, 
            SUM(barazumi) as totalBarazumi 
        FROM Item
        GROUP BY kenpinNo, itemCD
    """)
    fun getCSVdata(): List<CSVData>

}