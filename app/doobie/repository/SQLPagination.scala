package doobie.repository

import doobie._
import doobie.implicits._

/**
 * Pagination is a convenience to simply add limits and offsets to any query
 * Part of the motivation for this is using doobie's typechecker, which fails
 * unexpectedly for H2. H2 reports it requires a VARCHAR for limit and offset,
 * which seems wrong.
 */
trait SQLPagination {
  def limit[A](lim: Int)(fr: Fragment)(implicit read: Read[A]): doobie.Query0[A] =
    (fr ++ fr"LIMIT $lim").query

  def paginate[A](lim: Int, offset: Int)(fr: Fragment)(implicit read: Read[A]): doobie.Query0[A] =
    (fr ++ fr"LIMIT $lim OFFSET ${offset - 1}").query
}

object SQLPagination extends SQLPagination