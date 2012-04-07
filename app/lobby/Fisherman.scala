package lila
package lobby

import db.HookRepo
import memo.HookMemo
import model.{ Hook, DbGame }

import scalaz.effects._

final class Fisherman(
    hookRepo: HookRepo,
    hookMemo: HookMemo,
    socket: Lobby) {

  // DO delete in db
  def delete(hook: Hook): IO[Unit] = for {
    _ ← hide(hook)
    _ ← hookRepo removeId hook.id
  } yield ()

  // DO NOT delete in db
  def hide(hook: Hook): IO[Unit] = for {
    _ ← socket removeHook hook
    _ ← hookMemo remove hook.ownerId
  } yield ()

  // DO NOT insert in db (done on php side)
  def +(hook: Hook): IO[Unit] = for {
    _ ← socket addHook hook
    _ ← shake(hook)
  } yield ()

  def bite(hook: Hook, game: DbGame): IO[Unit] = for {
    _ ← hide(hook)
    _ ← socket.biteHook(hook, game)
  } yield ()

  // mark the hook as active, once
  def shake(hook: Hook): IO[Unit] = hookMemo put hook.ownerId

  def cleanup: IO[Unit] = for {
    hooks ← hookRepo unmatchedNotInOwnerIds hookMemo.keys
    _ ← (hooks map delete).sequence
  } yield ()
}