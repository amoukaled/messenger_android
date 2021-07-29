/* Copyright (C) 2021  Ali Moukaled
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example.messenger.models

/**
 * Authentication events to listen to in the StateFlow.
 */
sealed class AuthEvent<U>(val user: U?, val message: String?) {
    class Authenticated<U>(user: U) : AuthEvent<U>(user, null)

    class UnAuthenticated<U> : AuthEvent<U>(null, null)

    class Loading<U> : AuthEvent<U>(null, null)

    class Pending<U> : AuthEvent<U>(null, null)

    class CodeError<U>(message: String) : AuthEvent<U>(null, message)

    class NumberError<U>(message: String) : AuthEvent<U>(null, message)

    class GeneralError<U>(message: String) : AuthEvent<U>(null, message)

}
