-- ������� ��� ������ � ���������, � ������� ��� ���������
SELECT "item"."item_name" AS "items", "category"."category_title" AS "categories"
FROM "item", "category"
WHERE "item"."category_id"="category"."category_id";

-- ������� ��� ������ �� ����������� ������
SELECT "item"."item_name" 
FROM "item", (SELECT "item_id" 
				FROM "item__order" 
				WHERE "order_id"=?
			) AS "items__order" 
WHERE "item"."item_id"="items__order"."item_id";

--������� ��� ������ � ���������� �������� ������
SELECT "item__order"."order_id"
FROM "item__order"
WHERE "item__order"."item_id"=?;

--������� ��� ������, ���������� �� ��������� ���
SELECT "item"."item_name" 
FROM "item", (SELECT "item__order"."item_id"
				FROM "item__order", (SELECT "order_id" 
										FROM "order" 
										WHERE "order_updated" >= (NOW() - INTERVAL '1 HOUR')
									) AS "recent__orders"
				WHERE "item__order"."order_id"="recent__orders"."order_id"
			) AS "items__order"
WHERE "item"."item_id"="items__order"."item_id";

--������� ��� ������, ���������� �� �������
SELECT "item"."item_name" 
FROM "item", (SELECT "item__order"."item_id"
				FROM "item__order", (SELECT "order_id" 
										FROM "order" 
										WHERE "order_updated" >= (NOW() - INTERVAL '1 DAY')
									) AS "recent__orders"
				WHERE "item__order"."order_id"="recent__orders"."order_id"
			) AS "items__order"
WHERE "item"."item_id"="items__order"."item_id";

--������� ��� ������, ���������� �� �����
SELECT "item"."item_name" 
FROM "item", (SELECT "item__order"."item_id"
				FROM "item__order", (SELECT "order_id" 
										FROM "order" 
										WHERE "order_updated" >= (CURRENT_DATE - 1)
									) AS "recent__orders"
				WHERE "item__order"."order_id"="recent__orders"."order_id"
			) AS "items__order"
WHERE "item"."item_id"="items__order"."item_id";

--������� ��� ������ �� �������� ���������, ���������� �� ��������� ���.
SELECT "item"."item_name" 
FROM "item", (SELECT "item__order"."item_id"
				FROM "item__order", (SELECT "order_id" 
										FROM "order" 
										WHERE "order_updated" >= (NOW() - INTERVAL '1 HOUR')
									) AS "recent__orders"
				WHERE "item__order"."order_id"="recent__orders"."order_id"
			) AS "items__order"
WHERE "item"."item_id"="items__order"."item_id" AND "item"."category_id" = ?;

--������� ��� ������ �� �������� ���������, ���������� �� �������
SELECT "item"."item_name" 
FROM "item", (SELECT "item__order"."item_id"
				FROM "item__order", (SELECT "order_id" 
										FROM "order" 
										WHERE "order_updated" >= (NOW() - INTERVAL '1 DAY')
									) AS "recent__orders"
				WHERE "item__order"."order_id"="recent__orders"."order_id"
			) AS "items__order"
WHERE "item"."item_id"="items__order"."item_id" AND "item"."category_id" = ?;

--������� ��� ������ �� �������� ���������, ���������� �� �����
SELECT "item"."item_name" 
FROM "item", (SELECT "item__order"."item_id"
				FROM "item__order", (SELECT "order_id" 
										FROM "order" 
										WHERE "order_updated" >= (CURRENT_DATE - 1)
									) AS "recent__orders"
				WHERE "item__order"."order_id"="recent__orders"."order_id"
			) AS "items__order"
WHERE "item"."item_id"="items__order"."item_id" AND "item"."category_id" = ?;

--������� ��� ������, �������� ������� ���������� � �������� ������������������ ���� (��. LIKE)
SELECT "item_name"
FROM "item" 
WHERE "item_name" LIKE '?%';

--������� ��� ������, �������� ������� ������������� �������� ������������������� ���� (��. LIKE)
SELECT "item_name"
FROM "item" 
WHERE "item_name" LIKE '%?';

--������� ��� ������, �������� ������� �������� �������� ������������������ ���� (��. LIKE)
SELECT "item_name"
FROM "item" 
WHERE "item_name" LIKE '%?%';

--������� ������ ��������� � ���������� ������� � ������ ���������
SELECT "category_title", (SELECT COUNT(*) 
							FROM "item"
							WHERE "item"."category_id" = "category"."category_id"
						) AS "counter"
FROM "category";
				
--������� ������ ���� ������� � ���������� ������� � ������
SELECT "order_id", (SELECT COALESCE(SUM("item__order_quantity"),0)
								FROM "item__order"
								WHERE "item__order"."order_id" = "order"."order_id"
							) AS "counter"
FROM "order";
-----------------
--???????????????????������� ������ ���� ������� � ���������� �������, � ������� ������� ���� �����
-------???????????????????-----------�� �������(����� �������)
--������� ������ �������, ������������� �� ���� ������ � ��������� ��������� ������� � ������ �� ���
SELECT "order_id", (SELECT COALESCE(SUM("item__order_quantity"*"item"."item_price"),0)
						FROM "item__order", "item"
						WHERE "item__order"."order_id" = "order"."order_id" AND "item__order"."item_id" = "item"."item_id"
					) AS "total_price", "order_created"
FROM "order"
ORDER BY "order_created";

--������� ������ �������, ����, ���������� � ��������� ��������� ������� �� ��� � ������ � �������� ID
SELECT "item"."item_name", "item"."item_price", "item_price_quantity_total"."item__order_quantity", "item_price_quantity_total"."price_all"
FROM "item", (SELECT "item__order_quantity", "item__order"."item_id", "item__order_quantity"*"item"."item_price" AS "price_all"
				FROM "item__order", "item"
				WHERE "item__order"."order_id" = ? AND "item__order"."item_id" = "item"."item_id"
			) AS "item_price_quantity_total"
WHERE "item_price_quantity_total"."item_id" = "item"."item_id";

--��� ��������� ID ������ ������� ������ ���������, ������ �� ������� ������������ � ���� ������. ��� ������ 
--�� ��������� ������� ��������� ���������� � ��������� ��������� �������

--������� ������ ��������, ������� ���������� ������ �� ��������� � �������� ID �� ��������� 3 ���.
SELECT DISTINCT "customer"."customer_name" 
FROM "customer", (SELECT "recent__orders"."customer_id"
				FROM "item__order", (SELECT "order_id", "customer_id"
										FROM "order" 
										WHERE "order_updated" >= (CURRENT_DATE - 3)
									) AS "recent__orders",
									(SELECT "item"."item_id"
										FROM "item"
										WHERE "item"."category_id" = ?
									) AS "item__category"
				WHERE "item__order"."order_id"="recent__orders"."order_id" AND "item__order"."item_id" = "item__category"."item_id"
			) AS "select__customer"
WHERE "customer"."customer_id"="select__customer"."customer_id";

--������� ����� ���� ��������, ������������� ������ �� ��������� �����
SELECT DISTINCT "customer"."customer_name"
FROM "customer", (SELECT "customer_id"
					FROM "order" 
					WHERE "order_updated" >= (NOW() - INTERVAL '1 DAY')
				) AS "recent__orders"
WHERE "customer"."customer_id"="recent__orders"."customer_id";

--������� ���� ��������, ������������� ������, ���������� ����� � �������� ID
SELECT DISTINCT "customer"."customer_name"
FROM "customer", (SELECT "customer_id"
					FROM "order", (SELECT "item__order"."order_id"
										FROM "item__order"
										WHERE "item__order"."item_id" = ?
									) AS "temp"
					WHERE "order"."order_id" = "temp"."order_id"
				) AS "customer__orders"
WHERE "customer"."customer_id"="customer__orders"."customer_id";

--��� ������ ��������� ������� ��� �������� ����������� 
--� ������ category_image � ������� 'http://img.domain.com/category/<category_id>.jpg' ��� ���������� ���������,
-- � 'http://img.domain.com/category/<category_id>_disabled.jpg' ��� ����������
SELECT CASE WHEN "category_enabled"=TRUE 
				THEN 'http://img.domain.com/category/' || "category_id" || '.jpg'
				ELSE 'http://img.domain.com/category/' || "category_id" || '_disabled.jpg'
				END AS "url"
FROM "category";

--��� �������, ������� ���� �������� �� ��� ����� �� ���� ������� ����� ����������� ����� X ������, ���������� item_popular = TRUE, ��� ��������� � FALSE
UPDATE "item" 
SET "item_popular"= CASE 
						WHEN "item"."item_id"="item__quantity_popular"."item_id" THEN TRUE
						ELSE FALSE
					END
FROM (SELECT DISTINCT "item_id" 
							FROM "item__order" 
							WHERE "item__order_quantity">2
						) AS "item__quantity_popular"

--����� �������� ��� ��������� ID ��������� ���������� ���� category_enabled = TRUE, ��� ��������� � FALSE. �� ��������� WHERE
UPDATE "category" 
SET "category_enabled"= CASE 
						WHEN "category_id"= 1 THEN TRUE
						ELSE FALSE
					END;