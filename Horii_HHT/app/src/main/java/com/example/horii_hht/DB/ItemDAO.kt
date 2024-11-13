package com.example.horii_hht.DB

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface ItemDAO {
  @Insert
    fun insert(item: Item)
    @Query("SELECT * FROM Item")
    fun getItemAll(): List<Item>
  @Query("SELECT COUNT(DISTINCT itemCD) FROM Item")
 fun getDistinctItemCount(): Int

  @Query("SELECT SUM(suryo) FROM Item")
fun getTotalSuryo(): Int

  @Query("SELECT kenpinNo FROM Item LIMIT 1")
   fun getKenpinNo(): String?

    // suryoがzumiのアイテムの数を取得する
    @Query("SELECT COUNT(*) FROM Item WHERE suryo = 'zumi'")
    fun getCountOfZumiItems(): Int

    // 全行のzumi数をカウントする
    @Query("SELECT SUM(zumi) FROM Item")
    fun getTotalZumiCount(): Int
//JANかITFが一致するアイテムを取得する（複数）
    @Query("SELECT * FROM ITEM WHERE JAN = :jan OR ITF = :itf")
    fun getItemByCode(jan: String, itf: String): List<Item>
//JANかITFが一致するアイテムを取得する（一つだけ）
    @Query("SELECT * FROM ITEM WHERE jan = :jan OR itf = :itf LIMIT 1")
   fun getItemCode(jan: String, itf: String): Item?

   @Query("SELECT * FROM Item WHERE suryo - zumi > 0")
    fun getUnfinishedItems(): List<Item>

    @Query("SELECT * FROM Item WHERE kenpinNo = :kenpinNo")
    fun getItemsByKenpinNo(kenpinNo: String): List<Item>

    @Update
    fun update(item: Item)

    @Query("SELECT count(*) FROM Item")
    fun getItemCount(): Int

    @Query("DELETE FROM Item")
    fun deleteAllItems()

    @Query("""
    SELECT COUNT(*)
    FROM (
        SELECT itemCD
        FROM Item
        GROUP BY itemCD
        HAVING SUM(suryo) = SUM(zumi)
    ) AS matched_items
""")
    fun getCountOfMatchedItems(): Int

//    @Query("""    SELECT COUNT(*)
//    FROM Item i1
//    WHERE i1.zumi >= (
//        SELECT SUM(i2.suryo)
//        FROM Item i2
//        WHERE i2.itemCD = i1.itemCD
//    )
//""")
//    fun kenpinfinishItem(): List<Item>

}